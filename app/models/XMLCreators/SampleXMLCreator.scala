package models.XMLCreators

import models.Sample
import models.SampleFile
import models.SampleLIMSInfo
import models.persistance.SampleFileDAO
import models.persistance.SampleLIMSInfoDAO
import models.persistance.SampleSampleFileLinkDAO
import play.api.db.slick.Session
import java.io.IOException
import java.io.BufferedWriter
import java.nio.file._
import java.nio.charset.Charset

object SampleXMLCreator {

   def createSampleXML(directory: Path, samples: List[Sample]) = { implicit session: Session =>
      var filePath = directory.resolve("sample.xml")
      try {
         Files.createFile(filePath)
         writeToFile(filePath, samples)(session)
      } catch {
         case ex: FileAlreadyExistsException => {
            println("sample.xml file already exists at this location: " + filePath)
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
         case ex: IOException => {
            println("There was an error creating sample.xml!")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeToFile(filePath: Path, samples: List[Sample]) = { implicit session: Session =>
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeHeader(writer)
         for (sample <- samples) {
            val sampleFileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sample.name)(session)
            //I'm assuming that the donor is the same for all of the files tied to this sample 
            //Maybe we want to check this?
            val sampleLIMSInfoId = getFirstValidSampleFileLIMSInfoId(sampleFileIds)(session)
            if (sampleLIMSInfoId == 0) {
               println("ERROR: Sample " + sample + "is on the list of valid samples without a complete sample file!")
            }
            val donorId = SampleLIMSInfoDAO.getSampleLimsInfoById(sampleLIMSInfoId)(session).get.donor
            writeSample(writer, sample.name, donorId)
         }
         var xmlString = "</SAMPLE_SET>"
         writeLine(writer, xmlString)
         writer.close()
      } catch {
         case ex: IOException => {
            println("Error writing to sample.xml")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   @throws(classOf[IOException])
   def writeHeader(writer: BufferedWriter) {
      var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      writeLine(writer, xmlString)
      xmlString = "<SAMPLE_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.sample.xsd\">"
      writeLine(writer, xmlString)
   }

   @throws(classOf[IOException])
   def writeSample(writer: BufferedWriter, title: String, donorId: String) {
      var xmlString = "   <SAMPLE alias=\"" + title + "\" center_name=\"OICR_ICGC\">"
      writeLine(writer, xmlString)
      xmlString = "      <TITLE>" + title + "</TITLE>"
      writeLine(writer, xmlString)
      xmlString = "      <SAMPLE_NAME>"
      writeLine(writer, xmlString)
      xmlString = "         <TAXON_ID>9606</TAXON_ID>"
      writeLine(writer, xmlString)
      xmlString = "         <SCIENTIFIC_NAME>Homo sapiens</SCIENTIFIC_NAME>"
      writeLine(writer, xmlString)
      xmlString = "         <COMMON_NAME>human</COMMON_NAME>"
      writeLine(writer, xmlString)
      xmlString = "      </SAMPLE_NAME>"
      writeLine(writer, xmlString)
      xmlString = "      <SAMPLE_ATTRIBUTES>"
      writeLine(writer, xmlString)
      xmlString = "         <SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "            <TAG>Sample ID</TAG>"
      writeLine(writer, xmlString)
      xmlString = "            <VALUE>" + title + "</VALUE>"
      writeLine(writer, xmlString)
      xmlString = "         </SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "         <SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "            <TAG>Donor ID</TAG>"
      writeLine(writer, xmlString)
      xmlString = "            <VALUE>" + donorId + "</VALUE>"
      writeLine(writer, xmlString)
      xmlString = "         </SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "      </SAMPLE_ATTRIBUTES>"
      writeLine(writer, xmlString)
      xmlString = "   </SAMPLE>"
      writeLine(writer, xmlString)
   }

   @throws(classOf[IOException])
   def writeLine(writer: BufferedWriter, stringToWrite: String) {
      writer.write(stringToWrite, 0, stringToWrite.length)
      writer.newLine()
   }

   def getFirstValidSampleFileLIMSInfoId(sampleFileIds: List[Int]) = { implicit session: Session =>
      var sampleFileLIMSInfoId = 0
      for (sampleFileId <- sampleFileIds) {
         if (SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(session)) {
            sampleFileLIMSInfoId = SampleFileDAO.getSampleFileFromId(sampleFileId)(session).sampleLimsInfoId.get
         }
      }
      sampleFileLIMSInfoId
   }
}