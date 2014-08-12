package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.Sample
import models.SampleFile
import models.persistance.SampleDAO
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleFileDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO
import models.persistance.SampleSampleFileLinkDAO

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer

object XMLGeneration extends Controller {

   def viewXMLGenerationPage() = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined) {
         var releaseName = rs.request.session.get("releaseName").get
         var releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(rs.dbSession)
         var fileId = 0
         var fileName = ""
         if (rs.request.session.get("viewFilesSamples").isDefined && !rs.request.session.get("viewFilesSamples").get.equals("all")) {
            fileName = rs.request.session.get("viewFilesSamples").get
            fileId = TSVFileDAO.getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(rs.dbSession).get
         }

         var validSampleNames = ArrayBuffer[String]()
         var validSampleFileNames = ArrayBuffer[String]()

         var validSampleNamesToValidFileTypes: Map[String, List[String]] = getValidSampleNamesAndTypes(fileId, fileName, releaseId, releaseName)(rs.dbSession)

         validSampleNames ++= validSampleNamesToValidFileTypes.keys

         validSampleFileNames ++= getSampleFilesFromSamplesAndValidTypes(validSampleNamesToValidFileTypes)(rs.dbSession).map(s => s.fileName)

         Ok(views.html.xmlGeneration(validSampleNames.toArray, validSampleFileNames.toArray))
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases).withSession(rs.request.session)
      }
   }

   def generateXMLs() = Action { request =>
      //Generate XMLs here
      Redirect(routes.XMLSubmission.viewSubmissionPage)
   }

   def getValidSampleNamesAndTypes(tsvFileId: Int, tsvFileName: String, releaseId: Int, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var samplesFromTSVFiles: Map[String, List[String]] = Map() //Holds sample name as key and fileTypes to be used in the XML as the value
      if (tsvFileName != 0 && ReleaseTSVFileLinkDAO.tsvFileExistsInRelease(releaseId, tsvFileId)(session)) {
         samplesFromTSVFiles = getSampleNamesFromFile(tsvFileName, releaseName)(session)
      } else {
         samplesFromTSVFiles = getSampleNamesFromAllFiles(TSVFileDAO.getTSVFileNamesFromReleaseName(releaseName)(session), releaseName)(session)
      }
      samplesFromTSVFiles
   }

   def getSampleNamesFromAllFiles(filenames: List[String], releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var returnMap: Map[String, List[String]] = Map()
      for (filename <- filenames) {
         var tempMap = getSampleNamesFromFile(filename, releaseName)(session)
         for (sampleName <- tempMap.keys) {
            if (returnMap.contains(sampleName)) {
               var oldFileTypes = returnMap.get(sampleName).get
               returnMap -= sampleName
               var newFileTypes: ListBuffer[String] = new ListBuffer()
               newFileTypes ++= oldFileTypes
               for (fileType <- tempMap.get(sampleName).get) {
                  if (!newFileTypes.contains(fileType)) {
                     newFileTypes += fileType
                  }
               }
               returnMap += (sampleName -> newFileTypes.toList)
            } else {
               returnMap += (sampleName -> tempMap.get(sampleName).get)
            }
         }
      }
      returnMap
   }

   def getSampleFilesFromSamplesAndValidTypes(validSampleNamesToValidFileTypes: Map[String, List[String]]) = { implicit session: play.api.db.slick.Session =>
      var validSampleFiles = new ArrayBuffer[SampleFile]()
      for (sampleName <- validSampleNamesToValidFileTypes.keys) {
         var sampleFileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sampleName)(session)
         var validFileTypes = validSampleNamesToValidFileTypes.get(sampleName).get

         for (sampleFileId <- sampleFileIds) {
            var sampleFileType = SampleFileDAO.getFileTypeFromId(sampleFileId)(session)
            if (isValidFileType(validFileTypes, sampleFileType)) {
               if (SampleFileDAO.isSampleFileCompleteFromId(sampleFileId)(session)) {
                  validSampleFiles += SampleFileDAO.getSampleFileFromId(sampleFileId)(session)
               }
            }
         }
      }
      validSampleFiles
   }

   def getSampleNamesFromFile(fileName: String, releaseName: String) = { implicit session: play.api.db.slick.Session =>
      var returnMap: Map[String, List[String]] = Map()
      var samplesFromFile: List[Sample] = TSVFileSampleLinkDAO.getAllSamplesInTSVFile(fileName, releaseName)(session)
      var validFileType = TSVFileDAO.getFileTypeFromFileNameAndReleaseName(fileName, releaseName)(session)
      for (sample <- samplesFromFile) {
         if (SampleDAO.hasCompleteSampleFile(sample)(session)) {
            returnMap += (sample.name -> List(validFileType))
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