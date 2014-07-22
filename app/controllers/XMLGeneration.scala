package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.ReleaseTSVFileLinkDAO
import models.persistance.TSVFileSampleLinkDAO

import scala.collection.mutable.ArrayBuffer

object XMLGeneration extends Controller {

   def viewXMLGenerationPage() = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined) {
         var releaseId = ReleaseDAO.getReleaseIdFromName(rs.request.session.get("releaseName").get)(rs.dbSession)
         var fileId = 0
         var fileName = ""
         if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
            fileName = rs.request.session.get("viewFilesSamples").get
            fileId = TSVFileDAO.getTSVIdFromFileName(fileName)(rs.dbSession).get
         }

         if (fileId != 0 && ReleaseTSVFileLinkDAO.tsvFileExistsInRelease(releaseId, fileId)(rs.dbSession)) {
            var xmlNames = new ArrayBuffer[String]()
            for (sample <- TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName)(rs.dbSession)) {
               xmlNames += sample.name + ".xml"
            }
            Ok(views.html.xmlGeneration(xmlNames.toArray))
         } else {
            var xmlNames = new ArrayBuffer[String]()
            var fileIds = ReleaseTSVFileLinkDAO.getFileIdsFromReleaseId(releaseId)(rs.dbSession)
            for (fileId <- fileIds) {
               fileName = TSVFileDAO.getFileNameFromId(fileId)(rs.dbSession)
               for (sample <- TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName)(rs.dbSession)) {
                  xmlNames += sample.name + ".xml"
               }
            }
            Ok(views.html.xmlGeneration(xmlNames.toArray))
         }
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases).withSession(rs.request.session)
      }
   }

   def generateXMLs() = Action { request =>
      //Generate XMLs here
      Redirect(routes.XMLSubmission.viewSubmissionPage)
   }
}