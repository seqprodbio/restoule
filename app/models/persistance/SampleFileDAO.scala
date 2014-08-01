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

      val swidRegexString = "(SWID_[0-9]{4,6}_)"
      val sampleRegexString = "([A-Z]{3,5})_([0-9]{3,4}|[0-9][CR][0-9]{1,2})_(nn|[A-Z]{1}[a-z]{1})_([nRPXMCFE])"
      val libraryRegexString = sampleRegexString + "_(SE|PE|MP)_(nn|[0-9]{2,4}|[0-9]K)_(TS|EX|CH|BS|WG|TR|WT|SM|MR)"
      val sequencerRunNameRegexString = "(_NoIndex|_[0-9]*_[a-zA-Z0-9]*_[0-9]*[A-Z]*_[a-zA-Z0-9-]*_?([A-Z]{2,})?)"
      val sequencerRunNameWithNumberRegexString = "(_NoIndex|_[0-9]*_[a-zA-Z0-9]*_[A-Z0-9]*_[a-zA-Z0-9-]*_?([A-Z]{2,})?(_[0-9])?)"
      val laneRegexString = "(_[0-9]|_L[0-9]{3})"
      val readRegexString = "(_R[0-9])"
      val barcodeRegexString = "(_NoIndex|_[ACTG]{6,10})"

      val finalRegexString = "^" + swidRegexString + "?" + libraryRegexString + "(" + sequencerRunNameWithNumberRegexString + "(" + barcodeRegexString + "|_sd-seq|_mm)" + laneRegexString + "|" + sequencerRunNameRegexString + laneRegexString + barcodeRegexString + "?|" + barcodeRegexString + laneRegexString + sequencerRunNameWithNumberRegexString + ")" + readRegexString + "?(_[0-9]{3})?$"

      var regex = finalRegexString.r

      // Regex groups:
      // 1 Is the SWID
      // 2_3_4_5_6_7_8 is the library
      // 2_3_4_5 is the sample
      // 9 is the gathered sequencer run, barcode and lane I think
      // 10 is a possible sequencer run name
      // 11 is always null ..... I don't know what's supposed to be in it .....
      // 12 is always null ..... I don't know what's supposed to be in it .....
      // 13 is either null, _NoIndex, _sd-seq or _mm
      // 14 is either null or _NoIndex
      // 15 is a possible lane (either in Lnumber or number format)
      // 16 is a possible sequencer run name
      // 17 is a set of 2 letters (the end of sequence run name?)
      // 18 is a possible lane (either in Lnumber or number format)
      // 19 is either null or _NoIndex
      // 20 is a possible barcode
      // 21 is a possible lane
      // 22 is a possible sequencer run name
      // 23 is a set of 2 letters (the end of sequence run name?)
      // 24 is a ending to a sequence run name with a _[0-9]
      // 25 is the read
      // 26 is a mysterious string consisting of 3 numbers that sometimes matches the end of the line

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
         if (dataMatch.group(10) != null || dataMatch.group(16) != null || dataMatch.group(22) != null) {
            if (dataMatch.group(10) != null) {
               sequencerRunName = dataMatch.group(10).substring(1) //The substring is there to take out the  _
            } else if (dataMatch.group(16) != null) {
               sequencerRunName = dataMatch.group(16).substring(1)
            } else {
               sequencerRunName = dataMatch.group(22).substring(1)
            }
         }

         if (dataMatch.group(15) != null || dataMatch.group(18) != null || dataMatch.group(21) != null) {
            var matchNumber = 0
            if (dataMatch.group(15) != null) {
               matchNumber = 15
            } else if (dataMatch.group(18) != null) {
               matchNumber = 18
            } else {
               matchNumber = 21
            }
            var laneString = dataMatch.group(matchNumber).substring(1) //The substring is there to take out the  _
            if (laneString.charAt(0).equals('L')) {
               try {
                  lane = laneString.toInt
               } catch {
                  case e: Exception => lane = 0
               }
            } else {
               try {
                  lane = laneString.toInt
               } catch {
                  case e: Exception => lane = 0
               }
            }
         }

         if (dataMatch.group(20) != null && !dataMatch.group(20).equals("_NoIndex")) {
            barcode = dataMatch.group(20)
         }

         if (dataMatch.group(25) != null) {
            try {
               read = dataMatch.group(25).substring(2).toInt //The substring starts at 2 to take out the  _R
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