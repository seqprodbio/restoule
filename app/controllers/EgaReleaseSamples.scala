package controllers;

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import scala.collection.mutable.ListBuffer
import models.Sample
import models.SampleFile
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleDAO
import models.persistance.SampleFileDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.SampleSampleFileLinkDAO

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

         var samplesFromTSVFiles: Map[Sample, List[String]] = Map() //Holds sample as key and fileTypes to be displayed as the value
         if (rs.request.session.get("viewFilesSamples").isDefined && (filesInRelease.exists(_.equals(rs.request.session.get("viewFilesSamples").get)))) {
            samplesFromTSVFiles = getSamplesFromFile(rs.request.session.get("viewFilesSamples").get, releaseName, completenessOfSamples)(rs.dbSession)
         } else {
            samplesFromTSVFiles = getSamplesFromAllFiles(filesInRelease, releaseName, completenessOfSamples)(rs.dbSession)
         }

         var sampleFilesFromTSVFiles: Map[Sample, List[SampleFile]] = Map()
         for (sample <- samplesFromTSVFiles.keys) {
            var sampleFileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sample.name)(rs.dbSession)
            var sampleFiles: ListBuffer[SampleFile] = new ListBuffer()
            var validFileTypes = samplesFromTSVFiles.get(sample).get

            for (sampleFileId <- sampleFileIds) {
               var sampleFileType = SampleFileDAO.getFileTypeFromId(sampleFileId)(rs.dbSession)
               if (isValidFileType(validFileTypes, sampleFileType) && SampleFileDAO.isDataFile(SampleFileDAO.getSampleFileFromId(sampleFileId)(rs.dbSession).fileName)) {
                  if (SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(rs.dbSession) && (completenessOfSamples.equals("all") || completenessOfSamples.equals("complete"))) {
                     sampleFiles += SampleFileDAO.getSampleFileFromId(sampleFileId)(rs.dbSession)
                  } else if (!SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(rs.dbSession) && (completenessOfSamples.equals("all") || completenessOfSamples.equals("incomplete"))) {
                     sampleFiles += SampleFileDAO.getSampleFileFromId(sampleFileId)(rs.dbSession)
                  }
               }
            }

            sampleFilesFromTSVFiles += (sample -> sampleFiles.toList)
         }
         Ok(views.html.egaReleaseSamples(filesInRelease, sampleFilesFromTSVFiles, completenessOfSamples, rs.dbSession))
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

   def getSamplesFromAllFiles(filenames: List[String], releaseName: String, completenessType: String) = { implicit session: play.api.db.slick.Session =>
      var returnMap: Map[Sample, List[String]] = Map()
      for (filename <- filenames) {
         var tempMap = getSamplesFromFile(filename, releaseName, completenessType)(session)
         for (sample <- tempMap.keys) {
            if (returnMap.contains(sample)) {
               var oldFileTypes = returnMap.get(sample).get
               returnMap -= sample
               var newFileTypes: ListBuffer[String] = new ListBuffer()
               newFileTypes ++= oldFileTypes
               for (fileType <- tempMap.get(sample).get) {
                  if (!newFileTypes.contains(fileType)) {
                     newFileTypes += fileType
                  }
               }
               returnMap += (sample -> newFileTypes.toList)
            } else {
               returnMap += (sample -> tempMap.get(sample).get)
            }
         }
      }
      returnMap
   }

   def getSamplesFromFile(fileName: String, releaseName: String, completenessType: String) = { implicit session: play.api.db.slick.Session =>
      var returnMap: Map[Sample, List[String]] = Map()
      var samplesFromFile: List[Sample] = TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName, releaseName)(session)
      var validFileType = TSVFileDAO.getFileTypeFromFileNameAndReleaseName(fileName, releaseName)(session)
      for (sample <- samplesFromFile) {
         if ((completenessType.equals("all") || completenessType.equals("incomplete")) && !SampleDAO.hasCompleteSampleFile(sample)(session)) {
            returnMap += (sample -> List(validFileType))
         }
         if ((completenessType.equals("all") || completenessType.equals("complete")) && SampleDAO.hasCompleteSampleFile(sample)(session)) {
            returnMap += (sample -> List(validFileType))
         }
      }
      returnMap
   }

   def isValidFileType(validFileTypes: List[String], fileType: String): Boolean = {
      var valid = false
      for (validFileType <- validFileTypes) {
         if (validFileType.equals("all") || validFileType.equals(fileType)) {
            valid = true
         }
      }
      valid
   }
}