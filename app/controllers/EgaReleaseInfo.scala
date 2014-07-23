package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.ReleaseInfo
import models.persistance.ReleaseDAO

object EgaReleaseInfo extends Controller {

   val releaseInfoForm: Form[ReleaseInfo] = Form(mapping(
      "studyName" -> text,
      "studyAbstract" -> text)(ReleaseInfo.apply)(ReleaseInfo.unapply))

   def viewEgaReleaseInfo = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
         var releaseName = rs.request.session.get("releaseName").get
         var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
         var releaseInfo = ReleaseDAO.getReleaseInfoFromId(releaseId)(rs.dbSession)
         Ok(views.html.egaReleaseInfo(releaseInfo))
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases)
      }
   }

   def updateEgaReleaseInfo = DBAction { implicit rs =>
      releaseInfoForm.bindFromRequest.fold(
         formWithErrors => Ok("Bad form " + formWithErrors + "\n Please go back fix the errors with the form"),
         success => {
            var releaseName = rs.request.session.get("releaseName").get
            var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
            ReleaseDAO.updateReleaseInfoById(releaseId, success)(rs.dbSession)
            Redirect(routes.SampleSelection.viewSampleSelectionPage)
         })
   }
}