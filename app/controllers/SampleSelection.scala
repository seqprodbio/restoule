package controllers

import play.api._
import play.api.mvc._
import play.api.data.{ Form }
import play.api.data.Forms._
import java.io.File
import scala.io.Source

object SampleSelection extends Controller {

   def viewSampleSelectionPage() = Action { implicit request =>
      Ok(views.html.sampleSelection(None, None))
   }

   def upload() = Action(parse.multipartFormData) { implicit request =>
      var tsvFileOption: Option[File] = None
      var errorMessage = ""
      var filename = ""
      request.body.file("tsvFile").map { tsvFile =>
         if (tsvFile.contentType.get.equals("text/plain")) {
            filename = tsvFile.filename
            tsvFile.ref.moveTo(new File(s"./uploadedFiles/$filename"))
         } else {
            errorMessage = "Content does not seem to be a .tsv file"
         }
      }.getOrElse {
         errorMessage = "Please select a file to upload"
      }
      if (errorMessage.equals("")) {
         var tsvFile = Source.fromFile("./uploadedFiles/" + filename)
         var linesFromFile = tsvFile.getLines
         var tsvHeader: Option[List[String]] = Some(linesFromFile.next().split("\t").toList)
         var tsvContent: Option[List[List[String]]] = Some(getLineContentsFromLines(linesFromFile, 4))
         tsvFile.close()
         if (request.session.get("uploadErrorMessage").isDefined) {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent, false)).withSession(request.session - "uploadErrorMessage" + ("tsvFileName" -> filename))
         } else {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent)).withSession(request.session + ("tsvFileName" -> filename))
         }
      } else {
         Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(request.session + ("uploadErrorMessage" -> errorMessage))
      }
   }

   def processForm = Action { implicit request =>
      headerSelectionForm.bindFromRequest.fold(
         formWithErrors => {
            println(formWithErrors)
            Ok("Form has errors")
         },
         success => {
            var trueIndices = getTrueIndices(success)
            if (trueIndices.size > 0 && request.session.get("tsvFileName").isDefined) {
               var tsvFile = Source.fromFile("./uploadedFiles/" + request.session.get("tsvFileName").get)
               var numOfRows = tsvFile.getLines.size
               tsvFile.close()
               tsvFile = Source.fromFile("./uploadedFiles/" + request.session.get("tsvFileName").get)
               var rowsFromFile = tsvFile.getLines
               rowsFromFile.next()
               var tsvContent: List[List[String]] = getLineContentsFromLines(rowsFromFile, numOfRows - 1)
               var sampleNames: List[String] = List()
               for (index <- trueIndices) {
                  for (row <- tsvContent) {
                     sampleNames = row(index) :: sampleNames
                  }
               }
               if (request.session.get("emptyCheckboxMessage").isDefined) {
                  Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(request.session - "tsvFileName" - "emptyCheckboxMessage")
               } else {
                  Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(request.session - "tsvFileName")
               }
               //TODO: Store these sampleNames in database
            } else {
               Redirect(routes.SampleSelection.viewSampleSelectionPage).withSession(request.session + ("uploadErrorMessage" -> "Try again and select one or more columns this time"))
            }
         })
   }

   def getLineContentsFromLines(linesFromFile: Iterator[String], numOfLines: Int): List[List[String]] = {
      var returnList: List[List[String]] = List()
      var counter: Int = 0
      while (counter < numOfLines) {
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

   val headerSelectionForm = Form("value" -> list(boolean))
}