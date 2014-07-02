package controllers;

import play.api._
import play.api.mvc._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.ArrayBuilder

import models.Sample

object XMLSubmission extends Controller {

   val submissionForm = Form(single(
      "selectedButton" -> text))

   def viewSubmissionPage() = Action { implicit request =>
      if (!request.session.get("releaseName").isDefined) {
         Redirect(routes.EgaReleases.viewEgaReleases)
      } else {
         var generatedXMLs: Array[String] = getArrayOfGeneratedXMLS(request.session.get("releaseName").get)
         Ok(views.html.xmlSubmissionPage(generatedXMLs, areAllSamplesComplete(request.session.get("releaseName").get)))
      }
   }

   def processServerSubmission() = Action { implicit request =>
      submissionForm.bindFromRequest().fold(
         formHasErrors => Ok("The form had errors. These are the errors: " + formHasErrors),
         success => {
            if (success.equals("testServer")) {
               //Submit to test server code goes here
               Redirect(routes.XMLSubmission.viewSubmittedPage).withSession(request.session + ("serverSubmittedTo" -> "testServer"))
            } else {
               //Submit to real server code goes here
               Redirect(routes.XMLSubmission.viewSubmittedPage).withSession(request.session + ("serverSubmittedTo" -> "realServer"))
            }
         })
   }

   def viewSubmittedPage() = Action { implicit request =>
      Ok(views.html.xmlSubmittedPage())
   }

   def getArrayOfGeneratedXMLS(releaseName: String): Array[String] = {
      var xmlArray: Array[String] = Array("run.xml", "sample.xml")
      return xmlArray
   }

   def areAllSamplesComplete(releaseName: String): Boolean = {
      for (sample <- EgaReleaseSamples.getSamplesFromAllFiles(EgaReleaseSamples.getFileNamesInRelease(releaseName), "all")) {
         if (!sample.complete) {
            return false
         }
      }
      return true
   }
}