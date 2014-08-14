package models.XMLCreators

import java.nio.file._
import java.io.IOException
import java.io.BufferedWriter
import java.nio.charset.Charset

object DatasetXMLCreator {

   def createDatasetXML(directory: Path, datasetData: DatasetXMLData) {
      var filePath = directory.resolve("dataset.xml")
      try {
         Files.createFile(filePath)
         writeDatasetDataToFile(filePath, datasetData)
      } catch {
         case ex: FileAlreadyExistsException => {
            println("dataset.xml file already exists at this location: " + filePath)
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
         case ex: IOException => {
            println("There was an error creating dataset.xml!")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeDatasetDataToFile(filePath: Path, datasetData: DatasetXMLData) {
      val charset: Charset = Charset.forName("UTF-8")
      try {
         var writer: BufferedWriter = Files.newBufferedWriter(filePath, charset)
         writeDatasetHeader(writer)
         writeDataset(writer, datasetData)
         var xmlString = "</DATASETS>"
         SampleXMLCreator.writeLine(writer, xmlString)
         writer.close()
      } catch {
         case ex: IOException => {
            println("Error writing to dataset.xml")
            println(ex.getMessage())
            println(ex.printStackTrace())
         }
      }
   }

   def writeDatasetHeader(writer: BufferedWriter) {
      var xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "<DATASETS xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ftp://ftp.sra.ebi.ac.uk/meta/xsd/sra_1_5/EGA.dataset.xsd\">"
      SampleXMLCreator.writeLine(writer, xmlString)
   }

   def writeDataset(writer: BufferedWriter, datasetData: DatasetXMLData) {
      var xmlString = "   <DATASET alias=\"" + datasetData.alias + "\" center_name=\"OICR_ICGC\" broker_name=\"EGA\">"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <TITLE>" + datasetData.alias + "</TITLE>"
      SampleXMLCreator.writeLine(writer, xmlString)
      for (runRef <- datasetData.runRefs) {
         xmlString = "      <RUN_REF accession=\"" + runRef.accession + "\" refname=\"" + runRef.refname + "\" />"
         SampleXMLCreator.writeLine(writer, xmlString)
      }
      xmlString = "      <POLICY_REF refname=\"ICGC Data Access Agreements\" refcenter=\"OICR_ICGC\"/>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      <DATASET_LINKS>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         <DATASET_LINK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            <URL_LINK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "               <LABEL>ICGC Data Portal</LABEL>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "               <URL>http://dcc.icgc.org</URL>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "            </URL_LINK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "         </DATASET_LINK>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "      </DATASET_LINKS>"
      SampleXMLCreator.writeLine(writer, xmlString)
      xmlString = "   </DATASET>"
      SampleXMLCreator.writeLine(writer, xmlString)
   }
}