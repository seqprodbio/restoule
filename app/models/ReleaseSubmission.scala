package models

import models.XMLCreators.SubmissionXMLCreator
import scala.io.Source
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
import org.apache.http.entity.mime.MultipartEntityBuilder;
import scala.collection.mutable.ListBuffer

object ReleaseSubmission {

   case class AccessionInformation(resourceType: String, alias: String, accession: String)

   def submitToTestServer(directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      submitToServer("", directoryPath, releaseName)(session)
   }

   def submitToRealServer(directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      submitToServer("", directoryPath, releaseName)(session)
   }

   def submitToServer(server: String, directoryPath: Path, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      SubmissionXMLCreator.createSubmissionXML(directoryPath, releaseName + "1", "VALIDATE", List("sample.xml", "experiment.xml", "run.xml"))
      var response = submitFiles(server, List("sample.xml", "experiment.xml", "run.xml"), directoryPath, releaseName)
      println("Attempted to validate files, response is: \n\n" + response)
      if (isValid(response)) {
         SubmissionXMLCreator.createSubmissionXML(directoryPath, releaseName, "ADD", List("sample.xml", "experiment.xml", "run.xml"))
         response = submitFiles(server, List("sample.xml", "experiment.xml", "run.xml"), directoryPath, releaseName)
         println("Attempted to submit files to server, response is: \n\n" + response)
         if (isValid(response)) {
            "Release was submitted successfully!"
         } else {
            "Release was validated but was unable to be added"
         }
      } else {
         "Release was unable to be validated"
      }
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
            println("Error submitting the files in release " + releaseName + "!")
            ex.printStackTrace()
         }
      }
      return responseString
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