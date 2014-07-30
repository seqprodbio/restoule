package models.persistance

import models.SampleFile
import models.SampleFileTable

import play.api.db.slick.Config.driver.simple._

import scala.util.matching.Regex
import scala.collection.immutable.StringOps

object SampleFileDAO {

   val sampleFiles = TableQuery[SampleFileTable]

   def getAllSampleFiles() = { implicit session: Session =>
      sampleFiles.list
   }

   def getAllSampleFileNames() = { implicit session: Session =>
      sampleFiles.map(s => s.fileName).list
   }

   def getAllFTPSampleFiles() = { implicit session: Session =>
      sampleFiles.filter(s => s.origin === "ftp").list
   }

   def getAllLocalSampleFiles() = { implicit session: Session =>
      sampleFiles.filter(s => s.origin === "local").list
   }

   def doesSampleFileExist(path: String) = { implicit session: Session =>
      if (sampleFiles.filter(s => s.path === path).firstOption.isDefined) {
         true
      } else {
         false
      }
   }

   def getIdFromSampleFileName(fileName: String) = { implicit session: Session =>
      sampleFiles.filter(s => s.fileName === fileName).map(s => s.id).first
   }

   def getSampleFileFromId(id: Int) = { implicit session: Session =>
      sampleFiles.filter(s => s.id === id).first
   }
   
   def getFileTypeFromId(id: Int) = { implicit session: Session =>
      var fileName = sampleFiles.filter(s => s.id === id).map(s => s.fileName).first
      val regex = "^(.+?)\\.(bam|fastq\\.gz)\\.(gpg\\.md5|gpg|md5)$".r
      regex.findFirstMatchIn(fileName).get.group(2)
   }

   def createSampleFile(path: String, origin: String) = { implicit session: Session =>
      var pathParts = path.split("/")
      var fileName = pathParts(pathParts.length - 1)
      var fileNameParts = fileName.split("\\.")
      var fileNameWithoutExtension = fileNameParts(0)
      var swid = 0
      var sample = ""
      var library = ""
      var sequencerRunName = ""
      var lane = 0
      var barcode = ""
      var read = 0
      var regex = "^(SWID_[0-9]{4,6}_)?([A-Z]{3,5})_([0-9]{3,4}|[0-9][CR][0-9]{1,2})_(nn|[A-Z]{1}[a-z]{1})_([nRPXMCFE])_(SE|PE|MP)_(nn|[0-9]{2,4}|[0-9]K)_(TS|EX|CH|BS|WG|TR|WT|SM|MR)_(NoIndex|[0-9]*_[a-zA-Z0-9]*_[0-9]*_[a-zA-Z0-9-]*_?([A-Z]{2,})?)(_NoIndex|_sd-seq|_mm)?_([0-9]|L[0-9]{3})(_NoIndex)?_(R[0-9])(_[0-9]{3})?$".r
      if (regex.findFirstMatchIn(fileNameWithoutExtension).isDefined) {
         var dataMatch = regex.findFirstMatchIn(fileNameWithoutExtension).get
         if (dataMatch.group(1) != null) {
            try {
               swid = dataMatch.group(1).substring(5, dataMatch.group(1).length - 1).toInt
            } catch {
               case e: Exception => swid = 0
            }
         }
         if (dataMatch.group(2) != null && dataMatch.group(3) != null && dataMatch.group(4) != null && dataMatch.group(5) != null) {
            sample = dataMatch.group(2) + "_" + dataMatch.group(3) + "_" + dataMatch.group(4) + "_" + dataMatch.group(5)
            if (dataMatch.group(6) != null && dataMatch.group(7) != null && dataMatch.group(8) != null) {
               library = sample + "_" + dataMatch.group(6) + "_" + dataMatch.group(7) + "_" + dataMatch.group(8)
            }
         }
         if (dataMatch.group(9) != null) {
            sequencerRunName = dataMatch.group(9)
         }
         if (dataMatch.group(12) != null) {
            if (dataMatch.group(12).charAt(0).equals('L')) {
               try {
                  lane = dataMatch.group(12).substring(1).toInt
               } catch {
                  case e: Exception => lane = 0
               }
            } else {
               try {
                  lane = dataMatch.group(12).toInt
               } catch {
                  case e: Exception => lane = 0
               }
            }
         }
         if (dataMatch.group(13) != null && !dataMatch.group(13).equals("_NoIndex")) {
            barcode = dataMatch.group(13)
         }
         if (dataMatch.group(14) != null) {
            try {
               read = dataMatch.group(14).substring(1).toInt
            } catch {
               case e: Exception => read = 0
            }
         }

      }
      var newSampleFile = new SampleFile(None, fileName, path, origin, swid, sample, library, sequencerRunName, lane, barcode, read, new java.sql.Timestamp(System.currentTimeMillis()))
      sampleFiles.insert(newSampleFile)
   }

   def deleteAll() = { implicit session: Session =>
      sampleFiles.delete
   }
}