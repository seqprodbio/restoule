package controllers;

import play.api._
import play.api.mvc._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.Sample

object EgaReleaseSamples extends Controller {

   val completenessForm = Form(
      tuple(
         "all" -> boolean,
         "complete" -> boolean,
         "incomplete" -> boolean))

   val filenameForm = Form(
      single(
         "selectedFileName" -> text))

   def viewEgaReleaseSamples = Action { implicit request =>
      if (!request.session.get("releaseName").isDefined) {
         Redirect(routes.EgaReleases.viewEgaReleases)
      } else {
         var filesInRelease = getFileNamesInRelease(request.session.get("releaseName").get)

         var completenessOfSamples = ""
         if (request.session.get("completeness").isDefined) {
            completenessOfSamples = request.session.get("completeness").get
         } else {
            completenessOfSamples = "all"
         }

         var samplesFromFiles: List[Sample] = List()
         if (request.session.get("viewFilesSamples").isDefined && (filesInRelease.exists(_.equals(request.session.get("viewFilesSamples").get)))) {
            samplesFromFiles = getSamplesFromFile(request.session.get("viewFilesSamples").get, completenessOfSamples)
         } else {
            samplesFromFiles = getSamplesFromAllFiles(filesInRelease, completenessOfSamples)
         }
         Ok(views.html.egaReleaseSamples(filesInRelease, samplesFromFiles, completenessOfSamples))
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

   def getFileNamesInRelease(releaseName: String): List[String] = {
      return List("jcn_m.txt", "cnsm_m.txt")
   }

   def getSamplesFromAllFiles(filenames: List[String], completenessType: String): List[Sample] = {
      var returnList: List[Sample] = List()
      for (filename <- filenames) {
         returnList = returnList ::: getSamplesFromFile(filename, completenessType)
      }
      return returnList
   }

   def getSamplesFromFile(filename: String, completenessType: String): List[Sample] = {
      var returnList: List[Sample] = List()
      if (filename.equals("jcn_m.txt")) {
         if (completenessType.equals("all") || completenessType.equals("incomplete")) {
            returnList = returnList ::: List(new Sample("PCSI_0001", "", "", "", "", "", "", false))
         }
         if (completenessType.equals("all") || completenessType.equals("complete")) {
            returnList = returnList ::: List(new Sample("PCSI_0002", "", "", "", "", "", "", true))
         }
         if (completenessType.equals("all") || completenessType.equals("complete")) {
            returnList = returnList ::: List(new Sample("PCSI_0003", "", "", "", "", "", "", true))
         }
         if (completenessType.equals("all") || completenessType.equals("complete")) {
            returnList = returnList ::: List(new Sample("PCSI_0004", "", "", "", "", "", "", true))
         }
         if (completenessType.equals("all") || completenessType.equals("incomplete")) {
            returnList = returnList ::: List(new Sample("PCSI_0005", "", "", "", "", "", "", false))
         }
      }
      if (filename.equals("cnsm_m.txt")) {
         if (completenessType.equals("all") || completenessType.equals("incomplete")) {
            returnList = returnList ::: List(new Sample("PCSI_0006", "", "", "", "", "", "", false))
         }
         if (completenessType.equals("all") || completenessType.equals("complete")) {
            returnList = returnList ::: List(new Sample("PCSI_0007", "", "", "", "", "", "", true))
         }
      }
      return returnList
   }
}