package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.Sample
import models.SampleFile
import models.SampleLIMSInfo
import models.persistance.SampleDAO
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleFileDAO
import models.persistance.SampleLIMSInfoDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO
import models.persistance.SampleSampleFileLinkDAO
import models.XMLCreators.SampleXMLCreator
import models.XMLCreators.ExperimentXMLCreator
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import models.SampleLIMSInfo
import models.SampleLIMSInfo

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

   def generateXMLs() = DBAction { implicit rs =>
      var releaseName = rs.request.session.get("releaseName").get
      var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
      var fileId = 0
      var fileName = ""
      var validSampleNames = ArrayBuffer[String]()
      var directoryPath = Paths.get("./GeneratedXMLs/" + releaseName)
      var validSampleBuffer = ListBuffer[Sample]()
      var validSampleFiles = ArrayBuffer[SampleFile]()
      var validSampleNamesAndFileTypes = getValidSampleNamesAndTypes(fileId, fileName, releaseId, releaseName)(rs.dbSession)

      if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
         fileName = rs.request.session.get("viewFilesSamples").get
         fileId = TSVFileDAO.getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(rs.dbSession).get
      }

      validSampleNames ++= getValidSampleNamesAndTypes(fileId, fileName, releaseId, releaseName)(rs.dbSession).keys
      for (name <- validSampleNames) {
         validSampleBuffer += SampleDAO.getSampleFromSampleName(name)(rs.dbSession)
      }

      validSampleFiles ++= getSampleFilesFromSamplesAndValidTypes(validSampleNamesAndFileTypes)(rs.dbSession)

      var sampleNamesToLIMSInfo = getMapOfSampleAndLIMSInfoFromSampleFiles(validSampleFiles.toArray, validSampleNames.toArray)(rs.dbSession)

      createXMLDirectory(directoryPath)

      SampleXMLCreator.createSampleXML(directoryPath, validSampleBuffer.toList)(rs.dbSession)
      ExperimentXMLCreator.createExperimentXML(directoryPath, sampleNamesToLIMSInfo)(rs.dbSession)

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
      validSampleFiles
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

   def getMapOfSampleAndLIMSInfoFromSampleFiles(validSampleFile: Array[SampleFile], validSampleNames: Array[String]) = { implicit session: play.api.db.slick.Session =>
      var sampleAndLimsInfoMap = scala.collection.mutable.Map[String, SampleLIMSInfo]()
      for (sampleFile <- validSampleFile) {
         var parentSampleIds: List[Int] = SampleSampleFileLinkDAO.getSampleIdsFromFileName(sampleFile.fileName)(session)
         var parentSampleName = getValidParentSampleName(validSampleNames, parentSampleIds)(session)
         if (sampleFile.sampleLimsInfoId.isDefined) {
            var limsInfo: SampleLIMSInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
            var libraryStrategy = limsInfo.libraryStrategy
            var librarySource = limsInfo.librarySource
            var librarySelection = limsInfo.librarySelection
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
            sampleAndLimsInfoMap += (parentSampleName -> new SampleLIMSInfo(limsInfo.id, limsInfo.libraryName, limsInfo.donor, libraryStrategy, librarySource, librarySelection, limsInfo.created))
         } else {
            println("ERROR! SAMPLE FILE " + sampleFile.fileName + " IS IN RELEASE WITHOUT A SAMPLE LIMS INFO ID! IT IS INCOMPLETE!")
         }
      }
      sampleAndLimsInfoMap
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