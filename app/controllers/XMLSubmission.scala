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
import scala.collection.mutable.ArrayBuffer
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO

object XMLSubmission extends Controller {

   val submissionForm = Form(single(
      "selectedButton" -> text))

   def viewSubmissionPage() = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
         val releaseName = rs.request.session.get("releaseName").get
         var fileName = ""
         if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
            fileName = rs.request.session.get("viewFilesSamples").get
         } else {
            fileName = "all"
         }
         var generatedXMLs: Array[String] = getArrayOfGeneratedXMLS(releaseName, fileName)(rs.dbSession)
         Ok(views.html.xmlSubmissionPage(releaseName, fileName, generatedXMLs, areAllSamplesComplete(releaseName)(rs.dbSession)))
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

   def getArrayOfGeneratedXMLS(releaseName: String, fileName: String) = { implicit dbSession: play.api.db.slick.Session =>
      var xmlNames = new ArrayBuffer[String]()
      if (!fileName.equals("all")) {
         for (sample <- TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName, releaseName)(dbSession)) {
            xmlNames += sample.name + ".xml"
         }
      } else {
         var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(dbSession)
         var fileIds = ReleaseTSVFileLinkDAO.getFileIdsFromReleaseId(releaseId)(dbSession)
         for (fileId <- fileIds) {
            var tempFileName = TSVFileDAO.getFileNameFromId(fileId)(dbSession)
            for (sample <- TSVFileSampleLinkDAO.getAllSamplesInTSVFile(tempFileName, releaseName)(dbSession)) {
               xmlNames += sample.name + ".xml"
            }
         }
      }
      xmlNames.toArray
   }

   def areAllSamplesComplete(releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var returnValue = true
      for (sample <- EgaReleaseSamples.getSamplesFromAllFiles(TSVFileDAO.getTSVFileNamesFromReleaseName(releaseName)(session), releaseName, "all")(session).keys) {
         if (!sample.complete) {
            returnValue = false
         }
      }
      returnValue
   }
}