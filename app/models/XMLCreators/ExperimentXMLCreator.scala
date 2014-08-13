package models.XMLCreators

import play.api.db.slick.Session
import java.nio.file._
import java.io.IOException
import java.io.BufferedWriter
import java.nio.charset.Charset

import models.SampleLIMSInfo
import models.persistance.SampleFileDAO

object ExperimentXMLCreator {

   def createExperimentXML(directory: Path, experimentData: List[ExperimentXMLData]) = {
      var filePath = directory.resolve("experiment.xml")
      try {
         Files.createFile(filePath)
         writeToFile(filePath, experimentData)
      } catch {
         case ex: FileAlreadyExistsException => {
            println("experiment.xml file already exists at this location: " + filePath)
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
         case ex: IOException => {
            println("There was an error creating experiment.xml!")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeToFile(filePath: Path, experimentData: List[ExperimentXMLData]) = {
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeHeader(writer)
         for (experimentInfo <- experimentData) {
            writeExperiment(writer, experimentInfo)
         }
         var xmlString = "</EXPERIMENT_SET>"
         SampleXMLCreator.writeLine(writer, xmlString)
         writer.close()
      } catch {
         case ex: IOException => {
            println("Error writing to experiment.xml")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   @throws(classOf[IOException])
   def writeHeader(writer: BufferedWriter) {
      var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "<EXPERIMENT_SET xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/SRA.experiment.xsd\">"
      SampleXMLCreator.writeLine(writer, xmlString)
   }

   @throws(classOf[IOException])
   def writeExperiment(writer: BufferedWriter, experimentData: ExperimentXMLData) {
      var xmlString = "   <EXPERIMENT alias=\"" + experimentData.libraryName + "\" center_name=\"OICR_ICGC\">"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <STUDY_REF refname=\"Pancreatic Cancer OICR\" />"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <DESIGN>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <DESIGN_DESCRIPTION>" + experimentData.libraryName + "</DESIGN_DESCRIPTION>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <SAMPLE_DESCRIPTOR refname=\"" + experimentData.sampleName + "\" />"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <LIBRARY_DESCRIPTOR>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <LIBRARY_NAME>" + experimentData.libraryName + "</LIBRARY_NAME>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <LIBRARY_STRATEGY>" + experimentData.libraryStrategy + "</LIBRARY_STRATEGY>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <LIBRARY_SOURCE>" + experimentData.librarySource + "</LIBRARY_SOURCE>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <LIBRARY_SELECTION>" + experimentData.librarySelection + "</LIBRARY_SELECTION>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <LIBRARY_LAYOUT>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "               <PAIRED NOMINAL_LENGTH=\"" + experimentData.nominalLength + "\" />"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            </LIBRARY_LAYOUT>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         </LIBRARY_DESCRIPTOR>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      </DESIGN>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <PLATFORM>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <ILLUMINA>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <INSTRUMENT_MODEL>Illumina HiSeq 2000</INSTRUMENT_MODEL>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         </ILLUMINA>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      </PLATFORM>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "   </EXPERIMENT>"
      SampleXMLCreator.writeLine(writer, xmlString)

   }
}