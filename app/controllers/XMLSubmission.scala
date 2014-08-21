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
import models.persistance.SampleDAO
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.EGAAccessionDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO
import java.nio.file.Paths
import scala.collection.mutable.ListBuffer
import models.XMLCreators.RunReferenceData
import models.response.RestouleResponse
import models.response.ResponseHandler

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
          if (success.equals("validate")) {
            val resp = ReleaseSubmission.validate(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession)
            println(resp) // Yeah... not sure what this will look like.
            printResponseErrors(resp)
            //                  println(ReleaseSubmission.validate(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession))
            //                  Redirect(routes.XMLSubmission.viewSubmittedPage(resp)).withSession(rs.request.session + ("serverSubmittedTo" -> "validation"))
            Ok(views.html.xmlSubmittedPage()).withSession(rs.request.session + ("serverSubmittedTo" -> "validation"))
          } else if (success.equals("realServer")) {
            val responseString = ReleaseSubmission.submitToRealServer(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession)
            val restouleResponse = ResponseHandler.responseAccessions(responseString)
            printResponseErrors(restouleResponse)
            printAccessions(restouleResponse)
//            println(ReleaseSubmission.submitToRealServer(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName)(rs.dbSession))
            
            Redirect(routes.XMLSubmission.viewSubmittedPage()).withSession(rs.request.session + ("serverSubmittedTo" -> "realServer"))
          } else {
            var runs = getSubmittedRunReferenceDataFromRelease(releaseName)(rs.dbSession)
            println(ReleaseSubmission.submitDataset(Paths.get("./public/GeneratedXMLs/" + releaseName), releaseName, runs)(rs.dbSession))
            Redirect(routes.XMLSubmission.viewSubmittedPage).withSession(rs.request.session + ("serverSubmittedTo" -> "dataset submission"))
          }
        } else {
          Redirect(routes.EgaReleases.viewEgaReleases)
        }
      })
  }

  def printResponseErrors(restouleResponse: RestouleResponse) = {
    restouleResponse.errors.foreach { error =>
      Logger.error("xml submission error: " + error)
    }
  }

  def printAccessions(restouleResponse: RestouleResponse) = {
    restouleResponse.studyAbstract.foreach { accession =>
      Logger.info("xml submission response accession: " + accession.accession + " alias: " + accession.alias)
    }
  }

  def viewSubmittedPage() = Action { implicit request =>
    Ok(views.html.xmlSubmittedPage())
  }

  def getSubmittedRunReferenceDataFromRelease(releaseName: String) = { implicit session: play.api.db.slick.Session =>
    var runs = EGAAccessionDAO.getSubmittedRunsFromRelease(releaseName)(session)
    var runReferences = new ListBuffer[RunReferenceData]()
    for (run <- runs) {
      runReferences += new RunReferenceData(run.accession, run.refname)
    }
    runReferences.toList
  }
}