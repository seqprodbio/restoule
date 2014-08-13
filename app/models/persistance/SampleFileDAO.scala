package models.persistance

import models.SampleFile
import models.SampleFileTable

import play.api.db.slick.Config.driver.simple._

import scala.util.matching.Regex
import scala.collection.mutable.Map
import scala.collection.immutable.StringOps

object SampleFileDAO {

   val sampleFiles = TableQuery[SampleFileTable]

   val swidRegexString = "(SWID_[0-9]{4,6}_)"
   val sampleRegexString = "([A-Z]{3,5})_([0-9]{3,4}|[0-9][CR][0-9]{1,2})_(nn|[A-Z]{1}[a-z]{1})_([nRPXMCFE])"
   val libraryRegexString = sampleRegexString + "_(SE|PE|MP)_(nn|[0-9]{2,4}|[0-9]K)_(TS|EX|CH|BS|WG|TR|WT|SM|MR)"
   val sequencerRunNameRegexString = "(_NoIndex|_([0-9]{6})_[a-zA-Z0-9]*_[0-9]*[A-Z]*_[a-zA-Z0-9-]*_?([A-Z]{2,})?)"
   val sequencerRunNameWithNumberRegexString = "(_NoIndex|_([0-9]{6})_[a-zA-Z0-9]*_[A-Z0-9]*_[a-zA-Z0-9-]*_?([A-Z]{2,})?(_[0-9])?)"
   val laneRegexString = "(_[0-9]|_L[0-9]{3})"
   val readRegexString = "(_R[0-9])"
   val barcodeRegexString = "(_NoIndex|_[ACTG]{6,10})"

   val fileNameRegexString = "^" + swidRegexString + "?" + libraryRegexString + "(" + sequencerRunNameWithNumberRegexString + "(" + barcodeRegexString + "|_sd-seq|_mm)" + laneRegexString + "|" + sequencerRunNameRegexString + laneRegexString + barcodeRegexString + "?|" + barcodeRegexString + laneRegexString + sequencerRunNameWithNumberRegexString + ")" + readRegexString + "?(_[0-9]{3})?$"

   // Regex groups:
   // 1 Is the SWID
   // 2_3_4_5_6_7_8 is the library
   // 2_3_4_5 is the sample
   // 9 is the gathered sequencer run, barcode and lane I think
   // 10 is a possible sequencer run name
   // 11 is the beginning of the sequencer run name, the date part
   // 12 is always null ..... (I suspect it's supposed to be set of 2 letters (the end of sequence run name?))
   // 13 is always null ..... I don't know what's supposed to be in it .....
   // 14 is either null, _NoIndex, _sd-seq or _mm
   // 15 is either null or _NoIndex
   // 16 is a possible lane (either in Lnumber or number format)
   // 17 is a possible sequencer run name
   // 18 is the beginning of the sequencer run name, the date part
   // 19 is a set of 2 letters (the end of sequence run name?)
   // 20 is a possible lane (either in Lnumber or number format)
   // 21 is either null or _NoIndex
   // 22 is a possible barcode
   // 23 is a possible lane
   // 24 is a possible sequencer run name
   // 25 is the beginning of the sequencer run name, the date part
   // 26 is a set of 2 letters (the end of sequence run name?)
   // 27 is a ending to a sequence run name with a _[0-9]
   // 28 is the read
   // 29 is a mysterious string that sometimes matches the end of the line

   def getAllSampleFiles() = { implicit session: Session =>
      sampleFiles.list
   }

   def getAllSampleFileNames() = { implicit session: Session =>
      sampleFiles.map(s => s.fileName).list
   }

   def getAllFTPSampleFiles() = { implicit session: Session =>
      sampleFiles.filter(s => !s.origin.equals("local")).list
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

   def getAddedFileTypeFromId(id: Int) = { implicit session: Session =>
      var fileName = sampleFiles.filter(s => s.id === id).map(s => s.fileName).first
      val regex = "^(.+?)\\.(bam|fastq\\.gz)\\.(gpg\\.md5|gpg|md5)$".r
      regex.findFirstMatchIn(fileName).get.group(3)
   }

   def getDonor(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      if (sampleFile.sampleLimsInfoId.isDefined) {
         var libraryInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
         libraryInfo.donor
      } else {
         ""
      }
   }

   def getLibraryName(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      if (sampleFile.sampleLimsInfoId.isDefined) {
         var libraryInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
         libraryInfo.libraryName
      } else {
         sampleFile.library
      }
   }

   def getLibraryStrategy(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      if (sampleFile.sampleLimsInfoId.isDefined) {
         var libraryInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
         if (!libraryInfo.libraryStrategy.equals("")) {
            libraryInfo.libraryStrategy
         } else {
            if (getLibraryEndingFromId(id)(session).equals("WG")) {
               "WGS"
            } else if (getLibraryEndingFromId(id)(session).equals("EX")) {
               "WXS"
            } else {
               ""
            }
         }
      } else {
         ""
      }
   }

   def getLibrarySource(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      if (sampleFile.sampleLimsInfoId.isDefined) {
         var libraryInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
         if (!libraryInfo.librarySource.equals("")) {
            libraryInfo.librarySource
         } else {
            if (getLibraryEndingFromId(id)(session).equals("WG")) {
               "GENOMIC"
            } else if (getLibraryEndingFromId(id)(session).equals("EX")) {
               "GENOMIC"
            } else {
               ""
            }
         }
      } else {
         ""
      }
   }

   def getLibrarySelection(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      if (sampleFile.sampleLimsInfoId.isDefined) {
         var libraryInfo = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleFile.sampleLimsInfoId.get)(session).get
         if (!libraryInfo.librarySelection.equals("")) {
            libraryInfo.librarySelection
         } else {
            if (getLibraryEndingFromId(id)(session).equals("WG")) {
               "RANDOM"
            } else if (getLibraryEndingFromId(id)(session).equals("EX")) {
               "Hybrid Selection"
            } else {
               ""
            }
         }
      } else {
         ""
      }
   }

   def createSampleFile(path: String, sampleLimsInfoId: Int, origin: String) = { implicit session: Session =>
      var pathParts = path.split("/")
      var fileName = pathParts(pathParts.length - 1)
      var fileNameParts = fileName.split("\\.")
      var fileNameWithoutExtension = fileNameParts(0)
      var swid = 0
      var sample = ""
      var library = ""
      var sequencerRunName = ""
      var sequencerRunDate = ""
      var lane = 0
      var barcode = ""
      var read = 0

      var regex = fileNameRegexString.r

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
         if (dataMatch.group(10) != null || dataMatch.group(17) != null || dataMatch.group(24) != null) {
            if (dataMatch.group(10) != null) {
               sequencerRunName = dataMatch.group(10).substring(1) //The substring is there to take out the  _
            } else if (dataMatch.group(17) != null) {
               sequencerRunName = dataMatch.group(17).substring(1)
            } else {
               sequencerRunName = dataMatch.group(24).substring(1)
            }
         }

         if (dataMatch.group(11) != null || dataMatch.group(18) != null || dataMatch.group(25) != null) {
            if (dataMatch.group(11) != null) {
               sequencerRunDate = dataMatch.group(11)
            } else if (dataMatch.group(18) != null) {
               sequencerRunDate = dataMatch.group(18)
            } else {
               sequencerRunDate = dataMatch.group(25)
            }
         }

         if (dataMatch.group(16) != null || dataMatch.group(20) != null || dataMatch.group(23) != null) {
            var matchNumber = 0
            if (dataMatch.group(16) != null) {
               matchNumber = 16
            } else if (dataMatch.group(20) != null) {
               matchNumber = 20
            } else {
               matchNumber = 23
            }
            var laneString = dataMatch.group(matchNumber).substring(1) //The substring is there to take out the  _
            if (laneString.charAt(0).equals('L')) {
               try {
                  lane = laneString.substring(1).toInt
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

         if (dataMatch.group(22) != null && !dataMatch.group(22).equals("_NoIndex")) {
            barcode = dataMatch.group(22)
         }

         if (dataMatch.group(28) != null) {
            try {
               read = dataMatch.group(28).substring(2).toInt //The substring starts at 2 to take out the  _R
            } catch {
               case e: Exception => read = 0
            }
         }
      }
      var sampleLimsInfoIdOption: Option[Int] = None
      if (sampleLimsInfoId != 0) {
         sampleLimsInfoIdOption = Some(sampleLimsInfoId)
      }
      var newSampleFile = new SampleFile(None, sampleLimsInfoIdOption, fileName, path, origin, swid, sample, library, sequencerRunName, sequencerRunDate, lane, barcode, read, new java.sql.Timestamp(System.currentTimeMillis()))
      sampleFiles.insert(newSampleFile)
   }

   def getLibraryFromName(name: String): String = {
      var library = ""

      var regex = fileNameRegexString.r

      if (regex.findFirstMatchIn(name).isDefined) {
         var dataMatch = regex.findFirstMatchIn(name).get
         if (dataMatch.group(2) != null && dataMatch.group(3) != null && dataMatch.group(4) != null && dataMatch.group(5) != null && dataMatch.group(6) != null && dataMatch.group(7) != null && dataMatch.group(8) != null) {
            library = dataMatch.group(2) + "_" + dataMatch.group(3) + "_" + dataMatch.group(4) + "_" + dataMatch.group(5) + "_" + dataMatch.group(6) + "_" + dataMatch.group(7) + "_" + dataMatch.group(8)
         } else {
            println("One of the following is apparently null: ")
            println("2: " + dataMatch.group(2))
            println("3: " + dataMatch.group(3))
            println("4: " + dataMatch.group(4))
            println("5: " + dataMatch.group(5))
            println("6: " + dataMatch.group(6))
            println("7: " + dataMatch.group(7))
            println("8: " + dataMatch.group(8))
         }
      } else {
         println("Regex didn't match filename: " + name)
      }
      return library
   }

   def getNominalLengthFromLibraryName(name: String): Int = {
      val libraryRegex = libraryRegexString.r
      var nominalLength = 0

      if (libraryRegex.findFirstMatchIn(name).isDefined) {
         var dataMatch = libraryRegex.findFirstMatchIn(name).get
         try {
            nominalLength = dataMatch.group(6).toInt
         } catch {
            case e: Exception => nominalLength = 0
         }
      }
      nominalLength
   }

   def getLibraryEndingFromId(id: Int) = { implicit session: Session =>
      var library = getSampleFileFromId(id)(session).library
      library.substring(library.length - 2)
   }

   def getSequencerRunDateString(id: Int) = { implicit session: Session =>
      var sampleFile = getSampleFileFromId(id)(session)
      var dateNum = sampleFile.sequencerRunDate
      var date = "20" + dateNum.substring(0, 2) + "-" + dateNum.substring(2, 4) + "-" + dateNum.substring(4, 6) + "T00:00:00"
      date
   }

   def getMD5Path(sampleFileId: Int) = { implicit session: Session =>
      if (getAddedFileTypeFromId(sampleFileId)(session).equals("gpg")) {
         val fullFileName = getSampleFileFromId(sampleFileId)(session).fileName
         val strippedName = fullFileName.substring(fullFileName.length - 4)
         val md5Name = strippedName + ".md5"
         val md5SampleFile = sampleFiles.filter(s => s.fileName === md5Name).firstOption
         if (md5SampleFile.isDefined) {
            md5SampleFile.get.path
         } else {
            ""
         }
      }
   }

   def getGPGMD5Path(sampleFileId: Int) = { implicit session: Session =>
      if (getAddedFileTypeFromId(sampleFileId)(session).equals("gpg")) {
         val fullFileName = getSampleFileFromId(sampleFileId)(session).fileName
         val strippedName = fullFileName.substring(fullFileName.length - 4)
         val gpgMD5Name = strippedName + ".gpg.md5"
         val gpgMD5SampleFile = sampleFiles.filter(s => s.fileName === gpgMD5Name).firstOption
         if (gpgMD5SampleFile.isDefined) {
            gpgMD5SampleFile.get.path
         } else {
            ""
         }
      }
   }

   def deleteAll() = { implicit session: Session =>
      sampleFiles.delete
   }

   def isSampleFileComplete(sampleFile: SampleFile) = { implicit session: Session =>
      var libraryEnd = getLibraryEndingFromId(sampleFile.id.get)(session)
      if (!sampleFile.fileName.equals("") && sampleFile.sampleLimsInfoId.isDefined && (libraryEnd.equals("WG") || libraryEnd.equals("EX") || SampleLIMSInfoDAO.isComplete(sampleFile.sampleLimsInfoId.get)(session)) && doesNameMatchRegex(sampleFile.fileName) && getNominalLengthFromLibraryName(sampleFile.library) != 0) {
         true
      } else {
         false
      }
   }

   def isSampleFileCompleteFromId(id: Int) = { implicit session: Session =>
      isSampleFileComplete(getSampleFileFromId(id)(session))(session)
   }

   def doesNameMatchRegex(fileName: String): Boolean = {
      var fileNameParts = fileName.split("\\.")
      var fileNameWithoutExtension = fileNameParts(0)
      var regex = fileNameRegexString.r

      return regex.findFirstMatchIn(fileNameWithoutExtension).isDefined
   }
}