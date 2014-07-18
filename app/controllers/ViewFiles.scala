package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.List
import models.SampleFile
import models.SampleFileInfo
import models.FileRetrival
import models.persistance.SampleFileDAO
import models.FileRetrival
import models.persistance.LocalDirectoryDAO
import models.persistance.FTPCredentialsDAO
import scala.collection.mutable.Buffer

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
      for (ftpCredentials <- FTPCredentialsDAO.getAllFTPCredentials()(res.dbSession)) {
         var ftpFilePaths = FileRetrival.getFilePathsFromFTP(ftpCredentials.ftpSite, ftpCredentials.userName, ftpCredentials.password)
         SampleFileDAO.deleteAll()(res.dbSession)
         for (ftpFilePath <- ftpFilePaths) {
            SampleFileDAO.createSampleFile(ftpFilePath, "ftp")(res.dbSession)
         }
      }

      for (localDir <- LocalDirectoryDAO.getAllDirectories()(res.dbSession)) {
         var localFilePaths = FileRetrival.getFilePathsFromLocalDir(localDir.path)
         for (localFilePath <- localFilePaths) {
            SampleFileDAO.createSampleFile(localFilePath, "local")(res.dbSession)
         }
      }
      loading = false
      Redirect(routes.ViewFiles.viewFiles).withSession(res.request.session)
   }

   def processSampleFiles(sampleFiles: List[SampleFile]): HashSet[SampleFileInfo] = {
      var sampleFileInfo: HashSet[SampleFileInfo] = new HashSet[SampleFileInfo]();

      for (sampleFile: SampleFile <- sampleFiles) {
         var addedFileExtension = "" //This holds the .gpg, .md5, and .gpg.md5 part of the file extension
         var fileName = ""
         var originalFileExtension = "" //This holds the .bam or the .fastq.gz part of the file extension
         var fileNameParts = sampleFile.fileName.split("\\.")
         var length = fileNameParts.length
         var lastPart = fileNameParts(length - 1)
         var secondLastPart = fileNameParts(length - 2)
         var thirdLastPart = ""
         var fourthLastPart = ""
         if (length > 2) {
            thirdLastPart = fileNameParts(length - 3)
         }
         if (length > 3) {
            fourthLastPart = fileNameParts(length - 4)
         }

         if (lastPart.equals("bam") || (length > 2 && lastPart.equals("gz"))) {
            var fileNameEndPart = length - 1
            if (lastPart.equals("bam")) {
               fileNameEndPart -= 1
               originalFileExtension = lastPart
            } else { //if it equals gz meaning it's a fastq.gz file
               fileNameEndPart -= 2
               originalFileExtension = secondLastPart + "." + lastPart
            }
            var counter = 0;
            for (counter <- 0 to fileNameEndPart) {
               fileName += fileNameParts(counter) + "."
            }
            fileName = fileName.substring(0, fileName.length - 1)

         } else if ((lastPart.equals("md5") || lastPart.equals("gpg")) && (secondLastPart.equals("bam") || (length > 2 && secondLastPart.equals("gz") && thirdLastPart.equals("fastq")))) {
            addedFileExtension = lastPart
            var endPart = length - 1
            if (secondLastPart.equals("bam")) {
               endPart -= 2
               originalFileExtension = secondLastPart
            } else { //if it equals gz meaning it's a fastq.gz file
               endPart -= 3
               originalFileExtension = thirdLastPart + "." + secondLastPart
            }
            var counter = 0;
            for (counter <- 0 to endPart) {
               fileName += fileNameParts(counter) + "."
            }
            fileName = fileName.substring(0, fileName.length - 1)

         } else if (length > 2 && lastPart.equals("md5") && secondLastPart.equals("gpg") && (thirdLastPart.equals("bam") || (length > 3 && thirdLastPart.equals("gz") && fourthLastPart.equals("fastq")))) {
            addedFileExtension = secondLastPart + "." + lastPart
            var endPart = length - 1
            if (thirdLastPart.equals("bam")) {
               endPart -= 3
               originalFileExtension = thirdLastPart
            } else { //if it equals gz meaning it's a fastq.gz file
               endPart -= 4
               originalFileExtension = fourthLastPart + "." + thirdLastPart
            }

            var counter = 0;
            for (counter <- 0 to endPart) {
               fileName += fileNameParts(counter) + "."
            }
            fileName = fileName.substring(0, fileName.length - 1)
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