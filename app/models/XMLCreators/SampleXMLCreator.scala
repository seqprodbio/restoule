package models.XMLCreators

import play.api.db.slick.Session
import java.io.IOException
import java.io.BufferedWriter
import java.nio.file._
import java.nio.charset.Charset

object SampleXMLCreator {

   def createSampleXML(directory: Path, sampleData: List[SampleXMLData]) = {
      var filePath = directory.resolve("sample.xml")
      try {
         Files.createFile(filePath)
         writeToFile(filePath, sampleData)
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

   def writeToFile(filePath: Path, sampleData: List[SampleXMLData]) = {
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeHeader(writer)
         for (sample <- sampleData) {
            writeSample(writer, sample)
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
   def writeSample(writer: BufferedWriter, sampleData: SampleXMLData) {
      var xmlString = "   <SAMPLE alias=\"" + sampleData.sampleName + "\" center_name=\"OICR_ICGC\">"
      writeLine(writer, xmlString)
      xmlString = "      <TITLE>" + sampleData.sampleName + "</TITLE>"
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
      xmlString = "            <VALUE>" + sampleData.sampleName + "</VALUE>"
      writeLine(writer, xmlString)
      xmlString = "         </SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "         <SAMPLE_ATTRIBUTE>"
      writeLine(writer, xmlString)
      xmlString = "            <TAG>Donor ID</TAG>"
      writeLine(writer, xmlString)
      xmlString = "            <VALUE>" + sampleData.donorId + "</VALUE>"
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
}