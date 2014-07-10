package controllers;

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.ArrayBuilder

import models.Sample
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO

object XMLSubmission extends Controller {

   val submissionForm = Form(single(
      "selectedButton" -> text))

   def viewSubmissionPage() = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
         val releaseName = rs.request.session.get("releaseName").get
         var generatedXMLs: Array[String] = getArrayOfGeneratedXMLS(releaseName)
         Ok(views.html.xmlSubmissionPage(generatedXMLs, areAllSamplesComplete(releaseName)(rs.dbSession)))
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases)
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

   def areAllSamplesComplete(releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var returnValue = true
      for (sample <- EgaReleaseSamples.getSamplesFromAllFiles(TSVFileDAO.getTSVFileNamesFromReleaseName(releaseName)(session), "all")(session)) {
         if (!sample.complete) {
            returnValue = false
         }
      }
      returnValue
   }
}