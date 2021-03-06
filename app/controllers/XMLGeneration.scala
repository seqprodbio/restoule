package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.Sample
import models.SampleFile
import models.FileRetrival
import models.SampleLIMSInfo
import models.FTPCredentials
import models.persistance.SampleDAO
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleFileDAO
import models.persistance.EGAAccessionDAO
import models.persistance.SampleLIMSInfoDAO
import models.persistance.FTPCredentialsDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO
import models.persistance.SampleSampleFileLinkDAO
import models.XMLCreators.RunXMLData
import models.XMLCreators.RunXMLCreator
import models.XMLCreators.SampleXMLData
import models.XMLCreators.SampleFileData
import models.XMLCreators.SampleXMLCreator
import models.XMLCreators.ExperimentXMLData
import models.XMLCreators.ExperimentXMLCreator
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

object XMLGeneration extends Controller {

  def viewXMLGenerationPage() = DBAction { implicit rs =>
    if (rs.request.session.get("releaseName").isDefined) {
      var releaseName = rs.request.session.get("releaseName").get
      var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
      var fileId = 0
      var fileName = ""
      if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
        fileName = rs.request.session.get("viewFilesSamples").get
        fileId = TSVFileDAO.getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(rs.dbSession).get
      }

      var validSampleNames = ArrayBuffer[String]()
      var validSampleFileNames = ArrayBuffer[String]()

      var validSampleNamesToValidFileTypes: Map[String, List[String]] = getValidSampleNamesAndTypes(fileId, fileName, releaseId, releaseName)(rs.dbSession)

      validSampleNames ++= validSampleNamesToValidFileTypes.keys

      validSampleFileNames ++= getSampleFilesFromSamplesAndValidTypes(validSampleNamesToValidFileTypes)(rs.dbSession).map(s => s.fileName)

      Ok(views.html.xmlGeneration(validSampleNames.toArray, validSampleFileNames.toArray))
    } else {
      Redirect(routes.EgaReleases.viewEgaReleases).withSession(rs.request.session)
    }
  }

  /**
   * This method is called to generate the actual XML files. Interestingly, it takes
   * no arguments.
   */
  def generateXMLs() = DBAction { implicit rs =>
    var releaseName = rs.request.session.get("releaseName").get
    var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
    var fileId = 0
    var fileName = ""
    var directoryPath = Paths.get("./public/GeneratedXMLs/" + releaseName)
    var validSampleBuffer = ListBuffer[Sample]()
    var validSampleFiles = ArrayBuffer[SampleFile]()
    // Get a map of samples to an array of file types.
    var validSampleNamesAndFileTypes = getValidSampleNamesAndTypes(fileId, fileName, releaseId, releaseName)(rs.dbSession)

    if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
      fileName = rs.request.session.get("viewFilesSamples").get
      fileId = TSVFileDAO.getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(rs.dbSession).get
    }

    val validSampleNames = validSampleNamesAndFileTypes.keys
    for (name <- validSampleNames) {
      validSampleBuffer += SampleDAO.getSampleFromSampleName(name)(rs.dbSession)
    }

    validSampleFiles ++= getSampleFilesFromSamplesAndValidTypes(validSampleNamesAndFileTypes)(rs.dbSession)

    var sampleData = getSampleXMLData(validSampleBuffer.toList)(rs.dbSession)
    var experimentData = getExperimentXMLData(validSampleFiles.toArray, validSampleNames.toArray)(rs.dbSession)
    var runData: List[RunXMLData] = getRunXMLData(validSampleFiles)(rs.dbSession)

    createXMLDirectory(directoryPath)

    SampleXMLCreator.createSampleXML(directoryPath, sampleData)
    ExperimentXMLCreator.createExperimentXML(directoryPath, experimentData)
    RunXMLCreator.createRunXML(directoryPath, runData)

    Redirect(routes.XMLSubmission.viewSubmissionPage)
  }

  def getValidSampleNamesAndTypes(tsvFileId: Int, tsvFileName: String, releaseId: Int, releaseName: String) = { implicit session: play.api.db.slick.Session =>
    var samplesFromTSVFiles: Map[String, List[String]] = Map() //Holds sample name as key and fileTypes to be used in the XML as the value
    if (tsvFileName != 0 && ReleaseTSVFileLinkDAO.tsvFileExistsInRelease(releaseId, tsvFileId)(session)) {
      samplesFromTSVFiles = getSampleNamesFromFile(tsvFileName, releaseName)(session)
    } else {
      samplesFromTSVFiles = getSampleNamesFromAllFiles(TSVFileDAO.getTSVFileNamesFromReleaseName(releaseName)(session), releaseName)(session)
    }
    samplesFromTSVFiles
  }

  def getSampleNamesFromAllFiles(filenames: List[String], releaseName: String) = { implicit session: play.api.db.slick.Session =>
    var returnMap: Map[String, List[String]] = Map()
    for (filename <- filenames) {
      var tempMap = getSampleNamesFromFile(filename, releaseName)(session)
      for (sampleName <- tempMap.keys) {
        if (returnMap.contains(sampleName)) {
          var oldFileTypes = returnMap.get(sampleName).get
          returnMap -= sampleName
          var newFileTypes: ListBuffer[String] = new ListBuffer()
          newFileTypes ++= oldFileTypes
          for (fileType <- tempMap.get(sampleName).get) {
            if (!newFileTypes.contains(fileType)) {
              newFileTypes += fileType
            }
          }
          returnMap += (sampleName -> newFileTypes.toList)
        } else {
          returnMap += (sampleName -> tempMap.get(sampleName).get)
        }
      }
    }
    returnMap
  }

  def getSampleFilesFromSamplesAndValidTypes(validSampleNamesToValidFileTypes: Map[String, List[String]]) = { implicit session: play.api.db.slick.Session =>
    var validSampleFiles = new ArrayBuffer[SampleFile]()
    for (sampleName <- validSampleNamesToValidFileTypes.keys) {
      var sampleFileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sampleName)(session)
      var validFileTypes = validSampleNamesToValidFileTypes.get(sampleName).get

      for (sampleFileId <- sampleFileIds) {
        var sampleFileType = SampleFileDAO.getFileTypeFromId(sampleFileId)(session)
        if (isValidFileType(validFileTypes, sampleFileType)) {
          if (SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(session)) {
            validSampleFiles += SampleFileDAO.getSampleFileFromId(sampleFileId)(session)
          }
        }
      }
    }
    validSampleFiles = removeUploadedSampleFiles(validSampleFiles)(session)

    validSampleFiles
  }

  def removeUploadedSampleFiles(sampleFiles: ArrayBuffer[SampleFile]) = { implicit session: play.api.db.slick.Session =>
    var result = new ArrayBuffer[SampleFile]()
    sampleFiles.foreach { sampleFile =>
      if (!EGAAccessionDAO.sampleSubmitted(sampleFile)(session)) {
    	  result += sampleFile
      }
    }
    result
  }

  def getSampleNamesFromFile(fileName: String, releaseName: String) = { implicit session: play.api.db.slick.Session =>
    var returnMap: Map[String, List[String]] = Map()
    var samplesFromFile: List[Sample] = TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName, releaseName)(session)
    var validFileType = TSVFileDAO.getFileTypeFromFileNameAndReleaseName(fileName, releaseName)(session)
    for (sample <- samplesFromFile) {
      if (SampleDAO.hasCompleteSampleFile(sample)(session)) {
        returnMap += (sample.name -> List(validFileType))
      }
    }
    returnMap
  }

  def isValidFileType(validFileTypes: List[String], fileType: String): Boolean = {
    var valid = false
    for (validFileType <- validFileTypes) {
      if (validFileType.equals("all") || validFileType.equals(fileType)) {
        valid = true
      }
    }
    valid
  }

  def getSampleXMLData(validSamples: List[Sample]) = { implicit session: play.api.db.slick.Session =>
    var sampleXMLData = new ListBuffer[SampleXMLData]()
    for (sample <- validSamples) {
      val sampleFileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sample.name)(session)
      //I'm assuming that the donor is the same for all of the files tied to this sample 
      //Maybe we want to check this?
      val sampleLIMSInfoId = getFirstValidSampleFileLIMSInfoId(sampleFileIds)(session)
      if (sampleLIMSInfoId == 0) {
        println("ERROR: Sample " + sample + "is on the list of valid samples without a complete sample file!")
      }
      val donorId = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleLIMSInfoId)(session).get.donor
      val sampleName = sample.name
      if (!EGAAccessionDAO.existsWithName(sampleName)(session)) {
        sampleXMLData += new SampleXMLData(sampleName, donorId)
      } else {
        println("There already exists a previously submitted sample with alias: " + sampleName)
      }
    }
    sampleXMLData.toList
  }

  //This works, assuming all sample files attached to a sample have the same donor
  //Otherwise, you need to check if it's a valid sample file (if it's file type has been selected to be in the release)
  def getFirstValidSampleFileLIMSInfoId(sampleFileIds: List[Int]) = { implicit session: play.api.db.slick.Session =>
    var sampleFileLIMSInfoId = 0
    for (sampleFileId <- sampleFileIds) {
      if (SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(session)) {
        sampleFileLIMSInfoId = SampleFileDAO.getSampleFileFromId(sampleFileId)(session).sampleLimsInfoId.get
      }
    }
    sampleFileLIMSInfoId
  }

  def getExperimentXMLData(validSampleFiles: Array[SampleFile], validSampleNames: Array[String]) = { implicit session: play.api.db.slick.Session =>
    var experimentXMLData = new ListBuffer[ExperimentXMLData]()
    for (sampleFile <- validSampleFiles) {
      var parentSampleIds: List[Int] = SampleSampleFileLinkDAO.getSampleIdsFromFileName(sampleFile.fileName)(session)
      var parentSampleName = getValidParentSampleName(validSampleNames, parentSampleIds)(session)
      var limsInfo: SampleLIMSInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
      var libraryName = limsInfo.libraryName
      var libraryStrategy = limsInfo.libraryStrategy
      var librarySource = limsInfo.librarySource
      var librarySelection = limsInfo.librarySelection
      var nominalLength = SampleFileDAO.getNominalLengthFromLibraryName(libraryName)
      
      
      // This is required. But. It's not clear why some strategy, source and selection would have an empty string. Seems wrong.
      
      if (SampleFileDAO.getLibraryEndingFromId(sampleFile.id.get)(session).equals("WG")) {
        if (libraryStrategy.equals("")) {
          libraryStrategy = "WGS"
        }
        if (librarySource.equals("")) {
          librarySource = "GENOMIC"
        }
        if (librarySelection.equals("")) {
          librarySelection = "RANDOM"
        }
      } else if (SampleFileDAO.getLibraryEndingFromId(sampleFile.id.get)(session).equals("EX")) {
        if (libraryStrategy.equals("")) {
          libraryStrategy = "WXS"
        }
        if (librarySource.equals("")) {
          librarySource = "GENOMIC"
        }
        if (librarySelection.equals("")) {
          librarySelection = "Hybrid Selection"
        }
      }
      
      var libraryExists = false
      for (experimentData <- experimentXMLData) {
        if (experimentData.libraryName.equals(libraryName)) {
          libraryExists = true
        }
      }
      if (!libraryExists && !EGAAccessionDAO.existsWithName(libraryName)(session)) {
        experimentXMLData += new ExperimentXMLData(libraryName, parentSampleName, libraryStrategy, librarySource, librarySelection, nominalLength)
      } else if (EGAAccessionDAO.existsWithName(libraryName)(session)) {
        println("There already exists a previously submitted experiment with alias: " + libraryName)
      }
    }
    experimentXMLData.toList
  }

  def getValidParentSampleName(validSampleNames: Array[String], parentSampleIds: List[Int]) = { implicit session: play.api.db.slick.Session =>
    var parentName = ""
    for (id <- parentSampleIds) {
      var sampleName = SampleDAO.getSampleFromId(id)(session).name
      if (validSampleNames.contains(sampleName)) {
        parentName = sampleName
      }
    }
    parentName
  }

  def getRunXMLData(validSampleFiles: ArrayBuffer[SampleFile]) = { implicit session: play.api.db.slick.Session =>
    var runXMLData = new ListBuffer[RunXMLData]()
    for (sampleFile <- validSampleFiles) {
      if (SampleFileDAO.isDataFile(sampleFile.fileName)) {
        val checksumPath: String = SampleFileDAO.getMD5Path(sampleFile.id.get)(session)
        val checksumFile = SampleFileDAO.getSampleFileFromPath(checksumPath)(session)
        var checksum: String = ""
        val encryptedChecksumPath = SampleFileDAO.getGPGMD5Path(sampleFile.id.get)(session)
        val encryptedChecksumFile = SampleFileDAO.getSampleFileFromPath(encryptedChecksumPath)(session)
        var encryptedChecksum: String = ""
        var originalFileType = SampleFileDAO.getFileTypeFromId(sampleFile.id.get)(session)

        if (originalFileType.equals("fastq.gz")) {
          originalFileType = "fastq"
        }

        if (!checksumFile.origin.equals("local")) {
          val ftpCredentials = FTPCredentialsDAO.getFTPCredentialsFromSite(checksumFile.origin)(session)
          checksum = FileRetrival.getFileContentsFromFTP(ftpCredentials.ftpSite, ftpCredentials.userName, ftpCredentials.password, checksumPath).trim
        } else {
          checksum = FileRetrival.getFileContentsFromLocal(checksumPath).trim
        }
        if (!encryptedChecksumFile.origin.equals("local")) {
          val ftpCredentials = FTPCredentialsDAO.getFTPCredentialsFromSite(encryptedChecksumFile.origin)(session)
          encryptedChecksum = FileRetrival.getFileContentsFromFTP(ftpCredentials.ftpSite, ftpCredentials.userName, ftpCredentials.password, encryptedChecksumPath).trim
        } else {
          encryptedChecksum = FileRetrival.getFileContentsFromLocal(encryptedChecksumPath).trim
        }

        var fileData = new SampleFileData(sampleFile.path, originalFileType, "MD5", checksum, encryptedChecksum)
        var fileAlias = sampleFile.library + "_" + sampleFile.sequencerRunName + "_" + sampleFile.lane
        if (!sampleFile.barcode.equals("")) {
          fileAlias += sampleFile.barcode
        }
        var runDataWithAliasExists = false

        //Try to find a run with the alias expected by the file, if not create one
        for (runData <- runXMLData) {
          if (runData.alias.equals(fileAlias)) {
            var tempFiles = fileData :: runData.files
            var newRunData = new RunXMLData(runData.alias, runData.runDate, runData.experimentRef, tempFiles)
            runXMLData -= runData
            runXMLData += newRunData
            runDataWithAliasExists = true
          }
        }
        if (!runDataWithAliasExists && !EGAAccessionDAO.existsWithName(fileAlias)(session)) {
          runXMLData += new RunXMLData(fileAlias, SampleFileDAO.getSequencerRunDateString(sampleFile.id.get)(session), sampleFile.library, List(fileData))
        } else if (EGAAccessionDAO.existsWithName(fileAlias)(session)) {
          println("There already exists a previously submitted run with alias: " + fileAlias)
        }
      }
    }
    runXMLData.toList
  }

  /**
   * Generates a run alias (the unique name used by the EGA run XML fragment) from a sample file.
   */
  def runAlias(sampleFile: SampleFile): String = {
    sampleFile.library + "_" + sampleFile.sequencerRunName + "_" + sampleFile.lane + sampleFile.barcode
  }

  def createXMLDirectory(path: Path) = {
    if (Files.exists(path)) {
      Files.deleteIfExists(Paths.get(path.toString() + "/sample.xml"))
      Files.deleteIfExists(Paths.get(path.toString() + "/experiment.xml"))
      Files.deleteIfExists(Paths.get(path.toString() + "/run.xml"))
      Files.deleteIfExists(Paths.get(path.toString() + "/dataset.xml"))
      Files.deleteIfExists(Paths.get(path.toString() + "/submission.xml"))
    } else {
      Files.createDirectories(path)
    }
  }
}