package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._

import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleDAO

import java.io.File
import scala.io.Source
import scala.collection.mutable.ArrayBuffer

object SampleSelection extends Controller {

   def viewSampleSelectionPage() = DBAction { implicit rs =>
      if (rs.request.session.get("releaseName").isDefined && ReleaseDAO.releaseNameExists(rs.request.session.get("releaseName").get)(rs.dbSession)) {
         Ok(views.html.sampleSelection())
      } else {
         Redirect(routes.EgaReleases.viewEgaReleases)
      }
   }

   def upload() = DBAction(parse.multipartFormData) { implicit rs =>
      var errorMessage = ""
      var filename = ""
      var filePath = ""
      rs.request.body.file("tsvFile").map { tsvFile =>
         if (tsvFile.contentType.get.equals("text/plain")) {
            filename = tsvFile.filename
            var movedFile = new File(s"./uploadedFiles/$filename")
            tsvFile.ref.moveTo(movedFile)
            filePath = movedFile.getAbsolutePath()
         } else {
            errorMessage = "Content does not seem to be a .tsv file"
         }
      }.getOrElse {
         errorMessage = "Please select a file to upload"
      }
      if (errorMessage.equals("")) {
         var tsvFile = Source.fromFile("./uploadedFiles/" + filename)
         println(tsvFile.getClass())
         var linesFromFile = tsvFile.getLines
         var tsvHeader: Option[List[String]] = Some(linesFromFile.next().split("\t").toList)
         var tsvContent: Option[List[List[String]]] = Some(getLineContentsFromLines(linesFromFile, 4))
         tsvFile.close()
         var isColumnNameInSettingsArray: ArrayBuffer[Boolean] = new ArrayBuffer()
         for (headerName <- tsvHeader.get) {
            isColumnNameInSettingsArray.+=(isInSettings(headerName))
         }
         val releaseName = rs.request.session.get("releaseName").get
         if (TSVFileDAO.tsvFileExists(filename)(rs.dbSession)) {
            if (!TSVFileDAO.tsvFileExistsInRelease(releaseName, filename)(rs.dbSession)) {
               TSVFileDAO.addTSVFileToRelease(releaseName, filename)(rs.dbSession)
            }
         } else {
            TSVFileDAO.createTSVFile(releaseName, filename, filePath)(rs.dbSession)
         }
         if (rs.request.session.get("uploadErrorMessage").isDefined) {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent, isColumnNameInSettingsArray.toArray, false)).withSession(rs.request.session - "uploadErrorMessage" + ("tsvFileName" -> filename))
         } else {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent, isColumnNameInSettingsArray.toArray)).withSession(rs.request.session + ("tsvFileName" -> filename))
         }
      } else {
         Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(rs.request.session + ("uploadErrorMessage" -> errorMessage))
      }
   }

   def processForm = DBAction { implicit rs =>
      headerSelectionForm.bindFromRequest.fold(
         formWithErrors => {
            println(formWithErrors)
            Ok("Form has errors")
         },
         success => {
            var trueIndices = getTrueIndices(success)
            if (trueIndices.size > 0 && rs.request.session.get("tsvFileName").isDefined) {
               val tsvFileName = rs.request.session.get("tsvFileName").get
               var tsvFile = Source.fromFile("./uploadedFiles/" + tsvFileName)
               var numOfRows = tsvFile.getLines.size
               tsvFile.close()
               tsvFile = Source.fromFile("./uploadedFiles/" + tsvFileName)
               var rowsFromFile = tsvFile.getLines
               rowsFromFile.next()
               var tsvContent: List[List[String]] = getLineContentsFromLines(rowsFromFile, numOfRows - 1)
               tsvFile.close()
               var sampleNames: List[String] = List()
               for (index <- trueIndices) {
                  for (row <- tsvContent) {
                     sampleNames = row(index) :: sampleNames
                  }
               }

               for (sampleName <- sampleNames) {
                  if (!SampleDAO.sampleExists(tsvFileName, sampleName)(rs.dbSession)) {
                     SampleDAO.createSample(tsvFileName, sampleName, "", "", "", "", "", "", true)(rs.dbSession)
                  }
               }

               if (rs.request.session.get("emptyCheckboxMessage").isDefined) {
                  Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(rs.request.session - "tsvFileName" - "emptyCheckboxMessage")
               } else {
                  Redirect(routes.EgaReleaseSamples.viewEgaReleaseSamples).withSession(rs.request.session - "tsvFileName")
               }
            } else {
               Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(rs.request.session + ("uploadErrorMessage" -> "Try again and select one or more columns this time"))
            }
         })
   }

   def getLineContentsFromLines(linesFromFile: Iterator[String], numOfLines: Int): List[List[String]] = {
      var returnList: List[List[String]] = List()
      var counter: Int = 0
      while (counter < numOfLines && linesFromFile.hasNext) {
         returnList = returnList ::: List(linesFromFile.next().split("\t").toList)
         counter += 1
      }

      return returnList
   }

   def getTrueIndices(fullList: List[Boolean]): List[Int] = {
      var returnList: List[Int] = List()
      var counter = 0
      for (value <- fullList) {
         if (value == true) {
            returnList = returnList :+ counter
         }
         counter += 1
      }
      return returnList
   }

   def isInSettings(headerName: String): Boolean = {
      if (headerName.equals("analyzed_sample_id")) {
         return true
      }
      return false
   }

   val headerSelectionForm = Form("value" -> list(boolean))
}