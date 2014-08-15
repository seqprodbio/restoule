package models.XMLCreators

import java.io.IOException
import java.io.BufferedWriter
import java.nio.file._
import java.nio.charset.Charset

object SubmissionXMLCreator {

   def createSubmissionXML(directory: Path, submissionName: String, action: String, files: List[String]) = {
      var filePath = directory.resolve("submission.xml")
      try {
         if (Files.exists(filePath)) {
            Files.delete(filePath)
         }
         Files.createFile(filePath)
         writeSubmissionContentsToFile(filePath, submissionName, action, files)
      } catch {
         case ex: FileAlreadyExistsException => {
            println("submission.xml file already exists at this location: " + filePath)
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
         case ex: IOException => {
            println("There was an error creating submission.xml!")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeSubmissionContentsToFile(filePath: Path, submissionName: String, action: String, files: List[String]) = {
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeSubmissionHeader(writer)
         writeSubmissionContent(writer, submissionName, action, files)
         var xmlString = "</SUBMISSION_SET>"
         SampleXMLCreator.writeLine(writer, xmlString)
         writer.close()
      } catch {
         case ex: IOException => {
            println("Error writing to sample.xml")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeSubmissionHeader(writer: BufferedWriter) = {
      var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "<SUBMISSION_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.submission.xsd\">"
      SampleXMLCreator.writeLine(writer, xmlString)
   }

   //Note I assume that the files are in the form of type.xml (should hold true since we are the only ones generating these files
   def writeSubmissionContent(writer: BufferedWriter, submissionName: String, action: String, files: List[String]) = {
      var xmlString = "    <SUBMISSION alias=\"" + submissionName + "\" center_name=\"OICR_ICGC\" broker_name=\"EGA\">"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "        <ACTIONS>"
      SampleXMLCreator.writeLine(writer, xmlString)
      for (file <- files) {
         xmlString = "            <ACTION>"
         SampleXMLCreator.writeLine(writer, xmlString)
         xmlString = "                <" + action + " source=\"" + file + "\" schema=\"" + file.substring(0, file.length() - 4) + "\" />"
         SampleXMLCreator.writeLine(writer, xmlString)
         xmlString = "            </ACTION>"
         SampleXMLCreator.writeLine(writer, xmlString)
      }
      xmlString = "            <ACTION>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "                <PROTECT />"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            </ACTION>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "        </ACTIONS>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "    </SUBMISSION>"
      SampleXMLCreator.writeLine(writer, xmlString)
   }
}