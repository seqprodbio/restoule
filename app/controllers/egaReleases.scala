package controllers;

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.persistance.ReleaseDAO

object EgaReleases extends Controller {

   val addReleaseMapping = single("releaseName" -> text(minLength = 1))

   val addReleaseForm = Form(addReleaseMapping)

   val selectReleaseForm = Form("select" -> list(nonEmptyText))

   def viewEgaReleases = DBAction { implicit rs =>
      Ok(views.html.egaReleasesPage(ReleaseDAO.getReleaseNames()(rs.dbSession)))
   }

   def addEgaRelease = DBAction { implicit rs =>
      var newSession = rs.request.session
      if (rs.request.session.get("invalidViewRelease").isDefined) {
         newSession = newSession - "invalidViewRelease"
      }
      addReleaseForm.bindFromRequest().fold(
         formWithErrors => {
            newSession = newSession + ("invalidNewReleaseName" -> "The release name must not be empty!")
            Redirect(routes.EgaReleases.viewEgaReleases()).withSession(newSession)
         },
         releaseName => {
            if (ReleaseDAO.releaseNameExists(releaseName)(rs.dbSession)) {
               newSession = newSession + ("invalidNewReleaseName" -> "There already exists a release with that name!")
               Redirect(routes.EgaReleases.viewEgaReleases()).withSession(newSession)
            } else {
               ReleaseDAO.createRelease(releaseName, rs.request.session.get("username").get)(rs.dbSession)
               newSession = newSession + ("releaseName" -> releaseName)
               if (rs.request.session.get("invalidNewReleaseName").isDefined) {
                  newSession = newSession - "invalidNewReleaseName"
               }
               Redirect(routes.EgaReleaseInfo.viewEgaReleaseInfo).withSession(newSession)
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
}