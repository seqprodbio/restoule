package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.List
import models.SampleFile
import models.SampleFileInfo
import models.FileRetrival
import models.persistance.SampleDAO
import models.persistance.SampleFileDAO
import models.persistance.SampleLIMSInfoDAO
import models.persistance.LocalDirectoryDAO
import models.persistance.FTPCredentialsDAO
import models.persistance.SampleSampleFileLinkDAO
import org.json._
import models.lims.LimsJson
import models.lims.Sample

object ViewFiles extends Controller {

  var loading = false

  val optionsForm = Form(
    tuple(
      "all" -> boolean,
      "complete" -> boolean,
      "incomplete" -> boolean,
      "ftp" -> boolean,
      "local" -> boolean))

  def viewFiles() = DBAction { implicit res =>
    if (!loading) {
      var sampleFileInfo: HashSet[SampleFileInfo] = new HashSet[SampleFileInfo]()
      var sampleFiles: List[SampleFile] = List()
      if (!res.request.session.get("fileOriginLocation").isDefined || res.request.session.get("fileOriginLocation").isDefined && res.request.session.get("fileOriginLocation").get.equals("ftp")) {
        sampleFiles = SampleFileDAO.getAllFTPSampleFiles()(res.dbSession)
      } else {
        sampleFiles = SampleFileDAO.getAllLocalSampleFiles()(res.dbSession)
      }
      sampleFileInfo = processSampleFiles(sampleFiles)

      if (!res.request.session.get("completeness").isDefined || (res.request.session.get("completeness").isDefined && res.request.session.get("completeness").get.equals("all"))) {
        sampleFileInfo = sampleFileInfo
      } else if (res.request.session.get("completeness").get.equals("complete")) {
        sampleFileInfo = sampleFileInfo.filter(s => s.missingFileTypes.isEmpty)
      } else {
        sampleFileInfo = sampleFileInfo.filter(s => !s.missingFileTypes.isEmpty)
      }
      var sampleFileInfoBuffer: Buffer[SampleFileInfo] = sampleFileInfo.toBuffer.sortWith(sortSampleFileInfoByName)
      Ok(views.html.viewFiles(sampleFileInfoBuffer.toList)(res.request.session))
    } else {
      Redirect(routes.ViewFiles.loadingPage).withSession(res.request.session)
    }
  }

  def changeFileSelections() = Action { implicit request =>
    optionsForm.bindFromRequest.fold(
      formHasErrors => Ok("The form had errors! Here are the errors: " + formHasErrors + "\n Please return to the prevous page to correct them!"),
      success => {
        var completeness = "all"
        if (request.session.get("completeness").isDefined) {
          completeness = request.session.get("completeness").get
        }
        if (success._1) {
          completeness = "all"
        }
        if (success._2) {
          completeness = "complete"
        }
        if (success._3) {
          completeness = "incomplete"
        }
        var originSystem = "ftp"
        if (request.session.get("fileOriginLocation").isDefined) {
          originSystem = request.session.get("fileOriginLocation").get
        }
        if (success._4) {
          originSystem = "ftp"
        }
        if (success._5) {
          originSystem = "local"
        }
        var newSession = request.session
        if (newSession.get("fileOriginLocation").isDefined) {
          newSession = newSession - "fileOriginLocation"
        }
        if (newSession.get("completeness").isDefined) {
          newSession = newSession - "completeness"
        }
        Redirect(routes.ViewFiles.viewFiles()).withSession(newSession + ("completeness" -> completeness) + ("fileOriginLocation" -> originSystem));
      })
  }

  def loadingPage() = Action { implicit request =>
    if (!loading) {
      Redirect(routes.ViewFiles.viewFiles).withSession(request.session)
    } else {
      Ok("We are currently updating the table so that you can see the most up-to-date file listing possible. This may take between 5-15 minutes depending on the number of files on the FTP server. Please return to this page shortly in order to view the latest file listings")
    }
  }

  def updateFiles() = DBAction { implicit res =>
    loading = true
    SampleSampleFileLinkDAO.deleteAll()(res.dbSession)
    SampleFileDAO.deleteAll()(res.dbSession)
    val samples: Seq[Sample] = LimsJson.getSamplesFromUrl
    val sampleMap: Map[String, Sample] = LimsJson.getSampleMap(samples)
    for (ftpCredentials <- FTPCredentialsDAO.getAllFTPCredentials()(res.dbSession)) {
      var ftpFilePaths = FileRetrival.getFilePathsFromFTP(ftpCredentials.ftpSite, ftpCredentials.userName, ftpCredentials.password)
      for (ftpFilePath <- ftpFilePaths) {
        var pathParts = ftpFilePath.split("/")
        var fileName = pathParts(pathParts.length - 1)
        var fileNameParts = fileName.split("\\.")
        var fileNameWithoutExtension = fileNameParts(0)
        var libraryName = SampleFileDAO.getLibraryFromName(fileNameWithoutExtension)
        var sampleLimsInfoId = 0

        if (!SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).isDefined) {
          var librarySamples = LimsJson.getLibraryByName(SampleFileDAO.getLibraryFromName(fileNameWithoutExtension), samples)
          var donorSet = LimsJson.getDonorSampleSet(librarySamples, sampleMap)
          if (donorSet.size == 1) {
            var donor = donorSet.head

            getThreeLibraryAttributes(librarySamples.head, sampleMap) match {
              case (Some(strategy), Some(source), Some(selectionProcess)) => {
                SampleLIMSInfoDAO.createSampleLimsInfo(librarySamples.head.name, donor.name, Some(strategy.value), Some(source.value), Some(selectionProcess.value))(res.dbSession)
                sampleLimsInfoId = SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).get.id.get
              }
              case _ => {
                Logger.error(librarySamples.head.name + " is an unexpected library name. We expected to find " + libraryName + " We will be ignoring this sample!")
              }
            }
            //Note: If the libraryMap is empty, we let the sampleLimsInfoId be 0
            //This is because if we just make a link to the defaults, we may have an issue with the donor not existing(since the donor for each sample file linked to it would be different so we cannot make a single donor)
          } else {
            librarySamples match {
              case x :: xs => Logger.error("Too many donors for " + librarySamples.head.name + " from file: " + fileNameWithoutExtension)
              case Nil => Logger.error("No donors/libraries for " + SampleFileDAO.getLibraryFromName(fileNameWithoutExtension) + " from file: " + fileNameWithoutExtension)
            }
          }
        } else {
          sampleLimsInfoId = SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).get.id.get
        }
        SampleFileDAO.createSampleFile(ftpFilePath, sampleLimsInfoId, ftpCredentials.ftpSite)(res.dbSession)
        for (sampleName <- SampleDAO.getAllSampleNames()(res.dbSession)) {
          if (fileName.indexOf(sampleName) != -1 && !SampleSampleFileLinkDAO.linkExists(sampleName, fileName)(res.dbSession)) {
            SampleSampleFileLinkDAO.createLink(sampleName, fileName)(res.dbSession)
          }
        }
      }
    }

    for (localDir <- LocalDirectoryDAO.getAllDirectories()(res.dbSession)) {
      var localFilePaths = FileRetrival.getFilePathsFromLocalDir(localDir.path)
      for (localFilePath <- localFilePaths) {
        var pathParts = localFilePath.split("/")
        var fileName = pathParts(pathParts.length - 1)
        var fileNameParts = fileName.split("\\.")
        var fileNameWithoutExtension = fileNameParts(0)
        var libraryName = SampleFileDAO.getLibraryFromName(fileNameWithoutExtension)
        var sampleLimsInfoId = 0

        if (!SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).isDefined) {

          var librarySamples = LimsJson.getLibraryByName(SampleFileDAO.getLibraryFromName(fileNameWithoutExtension), samples)
          var donorSet = LimsJson.getDonorSampleSet(librarySamples, sampleMap)
          if (donorSet.size == 1) {
            var donor = donorSet.head

            getThreeLibraryAttributes(librarySamples.head, sampleMap) match {
              case (Some(strategy), Some(source), Some(selectionProcess)) => {
                SampleLIMSInfoDAO.createSampleLimsInfo(librarySamples.head.name, donor.name, Some(strategy.value), Some(source.value), Some(selectionProcess.value))(res.dbSession)
                sampleLimsInfoId = SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).get.id.get
              }
              // If the explicit attributes were not present then make a best guess based on name ending.
              case _ => {
                if (librarySamples.head.name.endsWith("EX"))
                  SampleLIMSInfoDAO.createSampleLimsInfo(librarySamples.head.name, donor.name, Some("WXS"), Some("GENOMIC"), Some("Hybrid Selection"))(res.dbSession)
                else if (librarySamples.head.name.endsWith("WG"))
                  SampleLIMSInfoDAO.createSampleLimsInfo(librarySamples.head.name, donor.name, Some("WGS"), Some("GENOMIC"), Some("RANDOM"))(res.dbSession)
                else
                  Logger.error(librarySamples.head.name + " is an unexpected library name. We expected to find " + libraryName + " We will be ignoring this sample!")
              }
            }
          } else {
            librarySamples match {
              case x :: xs => Logger.error("Too many donors for " + librarySamples.head.name + " from file: " + fileNameWithoutExtension)
              case Nil => Logger.error("No donors/libraries for " + SampleFileDAO.getLibraryFromName(fileNameWithoutExtension) + " from file: " + fileNameWithoutExtension)
            }
          }
          //Note: If the libraryMap is empty, we let the sampleLimsInfoId be 0
          //This is because if we just make a link to the defaults, we may have an issue with the donor not existing(since the donor for each sample file linked to it would be different so we cannot make a single donor)
        } else {
          sampleLimsInfoId = SampleLIMSInfoDAO.getSampleLimsInfoByLibraryName(libraryName)(res.dbSession).get.id.get
        }

        SampleFileDAO.createSampleFile(localFilePath, sampleLimsInfoId, "local")(res.dbSession)
        for (sampleName <- SampleDAO.getAllSampleNames()(res.dbSession)) {
          if (fileName.indexOf(sampleName) != -1 && !SampleSampleFileLinkDAO.linkExists(sampleName, fileName)(res.dbSession)) {
            SampleSampleFileLinkDAO.createLink(sampleName, fileName)(res.dbSession)
          }
        }
      }
    }
    loading = false
    Redirect(routes.ViewFiles.viewFiles).withSession(res.request.session)
  }

  def getThreeLibraryAttributes(sample: Sample, sampleMap: Map[String, Sample]) = {
    (LimsJson.getAttribute("Library Strategy", sample, sampleMap),
      LimsJson.getAttribute("Library Source", sample, sampleMap),
      LimsJson.getAttribute("Library Selection Process", sample, sampleMap))
  }

  def processSampleFiles(sampleFiles: List[SampleFile]): HashSet[SampleFileInfo] = {
    var sampleFileInfo: HashSet[SampleFileInfo] = new HashSet[SampleFileInfo]();

    for (sampleFile: SampleFile <- sampleFiles) {
      var addedFileExtension = "" //This holds the .gpg, .md5, and .gpg.md5 part of the file extension
      var fileName = ""
      var originalFileExtension = "" //This holds the .bam or the .fastq.gz part of the file extension

      val fileExtensionRegex = "^(.+?)\\.(bam|fastq\\.gz)\\.(gpg\\.md5|gpg|md5)$".r
      val matchOption = fileExtensionRegex.findFirstMatchIn(sampleFile.fileName)
      if (matchOption.isDefined) {
        fileName = matchOption.get.group(1)
        originalFileExtension = matchOption.get.group(2)
        addedFileExtension = matchOption.get.group(3)
      }

      if (!fileName.equals("")) {
        var oldSampleFileInfo = sampleFileInfo.find(s => s.name.equals(fileName))
        if (!oldSampleFileInfo.isDefined) {
          var missingTypes: ListBuffer[String] = new ListBuffer[String]()
          if (!addedFileExtension.equals("")) {
            if (!addedFileExtension.equals("gpg")) {
              missingTypes += originalFileExtension + ".gpg"
            }
            if (!addedFileExtension.equals("md5")) {
              missingTypes += originalFileExtension + ".md5"
            }
            if (!addedFileExtension.equals("gpg.md5")) {
              missingTypes += originalFileExtension + ".gpg.md5"
            }
          }
          var fileExtension = ""
          if (addedFileExtension.equals("")) {
            fileExtension = originalFileExtension
          } else {
            fileExtension = originalFileExtension + "." + addedFileExtension
          }
          var newSampleFileInfo = new SampleFileInfo(fileName, List(fileExtension), missingTypes.toList)
          sampleFileInfo.add(newSampleFileInfo)
        } else {
          var fileType = ""
          if (addedFileExtension.equals("")) {
            fileType = originalFileExtension
          } else {
            fileType = originalFileExtension + "." + addedFileExtension
          }
          var missingTypes: ListBuffer[String] = oldSampleFileInfo.get.missingFileTypes.to[ListBuffer]
          var existingTypes: ListBuffer[String] = oldSampleFileInfo.get.existingFileTypes.to[ListBuffer]
          if (!originalFileExtension.equals(existingTypes.head.substring(0, originalFileExtension.length))) {
            println(oldSampleFileInfo.get.name + " has both bam and fastq files we suspect!")
            println("Therefore, we are not going to be adding " + originalFileExtension + " file information to it!");
          } else if (!missingTypes.contains(fileType) && !existingTypes.contains(fileType)) {
            println(fileType + " is apparent not missing nor does it exist for filename: " + fileName);
          } else if (existingTypes.contains(fileType)) {
            println(fileType + " already exists for filename: " + fileName)
          } else { //Due to the previous 2 conditions, missingTypes must contain fileType and existingTypes must not
            existingTypes += fileType
            missingTypes -= fileType
            var newSampleFileInfo = new SampleFileInfo(fileName, existingTypes.toList, missingTypes.toList)
            sampleFileInfo -= oldSampleFileInfo.get
            sampleFileInfo += newSampleFileInfo
          }
        }
      }
    }

    return sampleFileInfo;
  }

  def sortSampleFileInfoByName(sampleFileInfo: SampleFileInfo, sampleFileInfo2: SampleFileInfo) = {
    sampleFileInfo.name.compareToIgnoreCase(sampleFileInfo2.name) < 0
  }
}