package models

import models.persistance.EGAAccessionDAO
import models.XMLCreators.DatasetXMLData
import models.XMLCreators.RunReferenceData
import models.XMLCreators.DatasetXMLCreator
import models.XMLCreators.SubmissionXMLCreator
import scala.io.Source
import scala.collection.mutable.ListBuffer
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.entity.ContentType
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.mime.MultipartEntityBuilder

object ReleaseSubmission {

   case class AccessionInformation(resourceType: String, alias: String, accession: String)

   def validate(directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var submissionAlias = getSubmissionAlias(releaseName)(session)
      //TODO: Change this so the url doesn't have to be hardcoded in
      validateOnServer("", directoryPath, List("sample.xml", "experiment.xml", "run.xml"), submissionAlias, releaseName)
   }

   def submitToRealServer(directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      //TODO: Change this so the url doesn't have to be hardcoded in
      submitToServer("", directoryPath, releaseName)(session)
   }

   def submitDataset(directoryPath: Path, releaseName: String, runs: List[RunReferenceData]) = { implicit session: play.api.db.slick.Session =>
      //TODO: Change this so it doesn't have to be hardcoded in
      var server = ""
      var submissionAlias = getSubmissionAlias(releaseName)(session)
      var datasetAlias = releaseName
      DatasetXMLCreator.createDatasetXML(directoryPath, new DatasetXMLData(datasetAlias, runs))
      var response = validateOnServer(server, directoryPath, List("sample.xml", "experiment.xml", "run.xml"), submissionAlias, releaseName)
      if (isValid(response)) {
         SubmissionXMLCreator.createSubmissionXML(directoryPath, releaseName, "ADD", List("dataset.xml"))
         response = submitFiles(server, List("dataset.xml"), directoryPath, releaseName)
         if (isValid(response)) {
            var accessionInformation = getAccessionNumbers(response)
            for (accessionInfo <- accessionInformation) {
               //If the release upload succeeded, that means that the accession numbers must be new
               EGAAccessionDAO.createAccession(accessionInfo.resourceType, accessionInfo.alias, accessionInfo.accession, releaseName)(session)
            }
         }
      }
      response
   }

   def validateOnServer(server: String, directoryPath: Path, files: List[String], submissionAlias: String, releaseName: String) = {
      SubmissionXMLCreator.createSubmissionXML(directoryPath, submissionAlias, "VALIDATE", List("sample.xml", "experiment.xml", "run.xml"))
      var response = submitFiles(server, files, directoryPath, releaseName)
      response
   }

   def submitToServer(server: String, directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var submissionAlias = getSubmissionAlias(releaseName)(session)
      var response = validateOnServer(server, directoryPath, List("sample.xml", "experiment.xml", "run.xml"), submissionAlias, releaseName)
      println("Attempted to validate files, response is: \n\n" + response)
      if (isValid(response)) {
         SubmissionXMLCreator.createSubmissionXML(directoryPath, submissionAlias, "ADD", List("sample.xml", "experiment.xml", "run.xml"))
         response = submitFiles(server, List("sample.xml", "experiment.xml", "run.xml"), directoryPath, releaseName)
         println("Attempted to submit files to server, response is: \n\n" + response)
         if (isValid(response)) {
            var accessionInformation = getAccessionNumbers(response)
            for (accessionInfo <- accessionInformation) {
               //If the release upload succeeded, that means that the accession numbers must be new
               EGAAccessionDAO.createAccession(accessionInfo.resourceType, accessionInfo.alias, accessionInfo.accession, releaseName)(session)
            }
         }
      }
      response
   }

   def submitFiles(server: String, filesToSubmit: List[String], directoryPath: Path, releaseName: String): String = {
      var responseString = ""
      var client = new DefaultHttpClient();
      var post = new HttpPost(server);
      try {
         var tempFiles = MultipartEntityBuilder.create()
         for (file <- filesToSubmit) {
            tempFiles.addBinaryBody(file.substring(0, file.length - 4).toUpperCase(), directoryPath.resolve(file).toFile(), ContentType.create("application/xml"), file)
         }
         tempFiles.addBinaryBody("SUBMISSION", directoryPath.resolve("submission.xml").toFile(), ContentType.create("application/xml"), "submission.xml")
         post.setEntity(tempFiles.build());

         var response = client.execute(post);
         var reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
         var line = reader.readLine();
         while (line != null) {
            responseString += line
            line = reader.readLine()
         }
      } catch {
         case ex: IOException => {
            println("Error submitting the files " + filesToSubmit + " in release " + releaseName + "!")
            ex.printStackTrace()
         }
      }
      return responseString
   }

   //This function exists because you need a unique alias for each submission so we have to get a name that hasn't been used before
   def getSubmissionAlias(releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var submissionName = releaseName + "_1"
      var getNumRegex = ".*_([0-9)*)".r
      while (EGAAccessionDAO.existsWithName(submissionName)(session)) {
         var numberAtEnd = 0
         //This is guaranteed exists since we set the submissionName above to follow this pattern
         var numString = getNumRegex.findFirstMatchIn(submissionName).get.group(1)
         numberAtEnd = numString.toInt
         submissionName = releaseName + "_" + (numberAtEnd + 1)
      }
      submissionName
   }

   def isValid(response: String): Boolean = {
      return true
   }

   def getAccessionNumbers(response: String): List[AccessionInformation] = {
      var accessionNumbers = ListBuffer[AccessionInformation]()
      //Code for extracting information from response goes here
      return accessionNumbers.toList
   }
}