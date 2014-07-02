package controllers;

import play.api._
import play.api.mvc._
import play.api.data.{ Form }
import play.api.data.Forms._

object EgaReleases extends Controller {

   val addReleaseMapping = single("releaseName" -> text(minLength = 1))

   val addReleaseForm = Form(addReleaseMapping)

   val selectReleaseForm = Form("select" -> list(nonEmptyText))

   def viewEgaReleases = Action { implicit request =>
      Ok(views.html.egaReleasesPage(getReleaseNames()))
   }

   def addEgaRelease = Action { implicit request =>
      var newSession = request.session
      if (request.session.get("invalidViewRelease").isDefined) {
         newSession = newSession - "invalidViewRelease"
      }
      addReleaseForm.bindFromRequest().fold(
         formWithErrors => {
            newSession = newSession + ("invalidNewReleaseName" -> "The release name must not be empty!")
            Redirect(routes.EgaReleases.viewEgaReleases()).withSession(newSession)
         },
         releaseName => {
            if (releaseNameAlreadyExists(releaseName)) {
               newSession = newSession + ("invalidNewReleaseName" -> "There already exists a release with that name!")
               Redirect(routes.EgaReleases.viewEgaReleases()).withSession(newSession)
            } else {
               newSession = newSession + ("releaseName" -> releaseName)
               if (request.session.get("invalidNewReleaseName").isDefined) {
                  newSession = newSession - "invalidNewReleaseName"
               }
               Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(newSession)
            }
         })
   }

   def openEgaRelease = Action { implicit request =>
      selectReleaseForm.bindFromRequest.fold(
         formWithErrors => {
            Redirect(routes.EgaReleases.viewEgaReleases())
         },
         values => {
            var newSession = request.session
            if (request.session.get("invalidNewReleaseName").isDefined) {
               newSession = newSession - "invalidNewReleaseName"
            }
            if (values.size > 0) {
               println(values(0))
               if (request.session.get("invalidViewRelease").isDefined) {
                  newSession = newSession - "invalidViewRelease"
               }
               newSession = newSession + ("releaseName" -> values(0))

               Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(newSession)
            } else {
               newSession = newSession + ("invalidViewRelease" -> "Please select a release to view")
               Redirect(routes.EgaReleases.viewEgaReleases()).withSession(newSession)
            }
         })
   }

   def releaseNameAlreadyExists(releaseName: String): Boolean = {
      if (getReleaseNames() contains releaseName) {
         return true
      } else {
         return false
      }
   }

   def getReleaseNames(): List[String] = {
      return List("Release 15 2013-11-21", "Release 15a 2013-10-01", "Release 13 2013-09-22")
   }
}