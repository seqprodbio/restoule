package controllers;

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.ArrayBuilder
import scala.collection.mutable.ArrayBuffer
import models.Sample
import models.ReleaseSubmission
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO
import java.nio.file.Paths

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
         //Might want to replace this in the future by actually checking what's in the directory
         var generatedXMLs: Array[String] = Array("sample.xml", "experiment.xml", "run.xml", "")
         Ok(views.html.xmlSubmissionPage(releaseName, fileName, generatedXMLs))
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases)
      }
   }

   def processServerSubmission() = DBAction { implicit rs =>
      submissionForm.bindFromRequest().fold(
         formHasErrors => Ok("The form had errors. These are the errors: " + formHasErrors),
         success => {
            if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
               val releaseName = rs.request.session.get("releaseName").get
               if (success.equals("testServer")) {
                  println(ReleaseSubmission.submitToTestServer(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession))
                  Redirect(routes.XMLSubmission.viewSubmittedPage).withSession(rs.request.session + ("serverSubmittedTo" -> "testServer"))
               } else {
                  println(ReleaseSubmission.submitToRealServer(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession))
                  Redirect(routes.XMLSubmission.viewSubmittedPage).withSession(rs.request.session + ("serverSubmittedTo" -> "realServer"))
               }
            } else {
               Redirect(routes.EgaReleases.viewEgaReleases)
            }
         })
   }

   def viewSubmittedPage() = Action { implicit request =>
      Ok(views.html.xmlSubmittedPage())
   }
}