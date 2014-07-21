package controllers;

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.Sample
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleDAO
import models.persistance.TSVFileSampleLinkDAO

object EgaReleaseSamples extends Controller {

   val completenessForm = Form(
      tuple(
         "all" -> boolean,
         "complete" -> boolean,
         "incomplete" -> boolean))

   val filenameForm = Form(
      single(
         "selectedFileName" -> text))

   def viewEgaReleaseSamples = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
         val releaseName = rs.request.session.get("releaseName").get
         var filesInRelease = TSVFileDAO.getTSVFileNamesFromReleaseName(releaseName)(rs.dbSession)

         var completenessOfSamples = ""
         if (rs.request.session.get("completeness").isDefined) {
            completenessOfSamples = rs.request.session.get("completeness").get
         } else {
            completenessOfSamples = "all"
         }

         var samplesFromFiles: List[Sample] = List()
         if (rs.request.session.get("viewFilesSamples").isDefined && (filesInRelease.exists(_.equals(rs.request.session.get("viewFilesSamples").get)))) {
            samplesFromFiles = getSamplesFromFile(rs.request.session.get("viewFilesSamples").get, completenessOfSamples)(rs.dbSession)
         } else {
            samplesFromFiles = getSamplesFromAllFiles(filesInRelease, completenessOfSamples)(rs.dbSession)
         }
         Ok(views.html.egaReleaseSamples(filesInRelease, samplesFromFiles, completenessOfSamples))
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases)
      }
   }

   def changeCompletenessDisplayed = Action { implicit request =>
      completenessForm.bindFromRequest().fold(
         formHasErrors => Ok("Errors! " + formHasErrors),
         success => {
            if (success._1) {
               Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(request.session + ("completeness" -> "all"))
            } else if (success._2) {
               Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(request.session + ("completeness" -> "complete"))
            } else {
               Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(request.session + ("completeness" -> "incomplete"))
            }
         })
   }

   def selectFileSamples = Action { implicit request =>
      filenameForm.bindFromRequest().fold(
         formHasErrors => Ok("The form had errors. Here are the details: " + formHasErrors),
         success => Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(request.session + ("viewFilesSamples" -> success)))
   }

   def getSamplesFromAllFiles(filenames: List[String], completenessType: String) = { implicit session: play.api.db.slick.Session =>
      var returnList: List[Sample] = List()
      for (filename <- filenames) {
         returnList = returnList ::: getSamplesFromFile(filename, completenessType)(session)
      }
      returnList.distinct
   }

   def getSamplesFromFile(filename: String, completenessType: String) = { implicit session: play.api.db.slick.Session =>
      var returnList: List[Sample] = List()
      var samplesFromFile: List[Sample] = TSVFileSampleLinkDAO.getAllSamplesInTSVFile(filename)(session)
      for (sample <- samplesFromFile) {
         if ((completenessType.equals("all") || completenessType.equals("incomplete")) && !sample.complete) {
            returnList = returnList ::: List(sample)
         }
         if ((completenessType.equals("all") || completenessType.equals("complete")) && sample.complete) {
            returnList = returnList ::: List(sample)
         }
      }
      returnList
   }
}