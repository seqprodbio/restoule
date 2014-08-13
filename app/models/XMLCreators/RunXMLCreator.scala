package models.XMLCreators

import play.api.db.slick.Session
import java.nio.file._
import java.io.IOException
import java.io.BufferedWriter
import java.nio.charset.Charset

import models.SampleLIMSInfo
import models.persistance.SampleFileDAO

object RunXMLCreator {

   def createRunXML(directory: Path, runData: List[RunXMLData]) = {
      var filePath = directory.resolve("run.xml")
      try {
         Files.createFile(filePath)
         writeRunContentsToFile(filePath, runData)
      } catch {
         case ex: FileAlreadyExistsException => {
            println("run.xml file already exists at this location: " + filePath)
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
         case ex: IOException => {
            println("There was an error creating run.xml!")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeRunContentsToFile(filePath: Path, runData: List[RunXMLData]) = {
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeRunHeader(writer)
         for (runInfo <- runData) {
            writeRun(writer, runInfo)
         }
         var xmlString = "</RUN_SET>"
         SampleXMLCreator.writeLine(writer, xmlString)
         writer.close()
      } catch {
         case ex: IOException => {
            println("Error writing to run.xml")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeRunHeader(writer: BufferedWriter) = {
      var xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "<RUN_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.run.xsd\">"
      SampleXMLCreator.writeLine(writer, xmlString)
   }

   def writeRun(writer: BufferedWriter, runData: RunXMLData) = {
      var xmlString = "   <RUN alias=\"" + runData.alias + "\" center_name=\"OICR_ICGC\" run_date=\"" + runData.runDate + "\">"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <EXPERIMENT_REF refname=\"" + runData.experimentRef + "\" />"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <DATA_BLOCK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <FILES>"
      SampleXMLCreator.writeLine(writer, xmlString)
      for (fileData <- runData.files) {
         xmlString = "            <FILE filename=\"" + fileData.filePath + "\" filetype=\"" + fileData.fileType + "\" checksum_method=\"" + fileData.checksumMethod + "\" checksum=\"" + fileData.encryptedChecksum + "\" unencrypted_checksum=\"" + fileData.checksum + "\" />"
         SampleXMLCreator.writeLine(writer, xmlString)
      }
      xmlString = "         </FILES>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      </DATA_BLOCK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "   </RUN>"
      SampleXMLCreator.writeLine(writer, xmlString)
   }
}