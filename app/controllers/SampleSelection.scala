package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.data.{ Form }
import play.api.data.Forms._
import models.persistance.SampleFileDAO
import models.persistance.TSVFileSampleLinkDAO
import models.persistance.ReleaseDAO
import models.persistance.TSVFileDAO
import models.persistance.SampleDAO
import models.persistance.PreferredHeaderDAO
import java.io.File
import java.util.Scanner
import scala.io.Source
import scala.util.matching.Regex
import scala.collection.mutable.ArrayBuffer
import models.persistance.SampleSampleFileLinkDAO
import models.persistance.ReleaseTSVFileLinkDAO

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
      var fileName = ""
      var filePath = ""
      rs.request.body.file("tsvFile").map { tsvFile =>
         if (tsvFile.contentType.get.equals("text/plain")) {
            fileName = tsvFile.filename
            var tempFile = new File(s"./uploadedFiles/temp.txt")
            if (tempFile.exists()) {
               tempFile.delete()
            }
            var fileLocation = new File(s"./uploadedFiles/$fileName")
            tsvFile.ref.moveTo(tempFile)
            var moved = false
            while (!moved) { //This is to deal with someone uploading a file with a name that already exists (including if it has different file content, in which case, we add a (number) at the end)
               if (!fileLocation.exists()) {
                  tempFile.renameTo(fileLocation)
                  filePath = fileLocation.getAbsolutePath()
                  moved = true
               } else {
                  if (fileContentsAreEqual(tempFile, fileLocation)) {
                     filePath = fileLocation.getAbsolutePath()
                     moved = true
                  } else {
                     var fileNameParts: Array[String] = fileName.split("\\.")
                     val regex = "\\([0-9]+\\)".r
                     if (regex.findAllIn(fileNameParts(0)).hasNext) { //This will work if no one has tsvfiles with a (number) in them
                        var resultMatch = regex.findAllIn(fileNameParts(0)).matchData.next
                        var resultString = resultMatch.matched
                        var number = resultString.substring(1, resultString.length - 1).toInt
                        number = number + 1
                        fileNameParts(0) = fileNameParts(0).substring(0, resultMatch.start) + "(" + number + ")" + fileNameParts(0).substring(resultMatch.end, fileNameParts(0).length)
                        fileName = fileNameParts.mkString(".")
                        fileLocation = new File(s"./uploadedFiles/$fileName")
                        //Do some more checking that we've actually added a () and then replace the number in teh () and put the filename back together and then change the File
                     } else {
                        fileNameParts(0) = fileNameParts(0) + "(1)"
                        fileName = fileNameParts.mkString(".")
                        fileLocation = new File(s"./uploadedFiles/$fileName")
                     }
                  }
               }
            }
         } else {
            errorMessage = "Content does not seem to be a .tsv file"
         }
      }.getOrElse {
         errorMessage = "Please select a file to upload"
      }
      var fileType = ""
      sampleTypeSelectionForm.bindFromRequest.fold(
    	  formWithErrors =>{
    	     errorMessage = "Error with file type selection. Please try again"
    	     println(formWithErrors)
    	  },
    	  success => {
    	    println(success)
    	    fileType = success
    	    if (fileType.charAt(0).equals('.')) {
    	    	fileType = fileType.substring(1)
    	    }
    	  }
      )
      
      if(TSVFileDAO.tsvFileExistsInRelease(rs.request.session.get("releaseName").get, fileName)(rs.dbSession)){
         errorMessage = "File already exists in release!"
      }
      
      if (errorMessage.equals("")) {
         var tsvFile = Source.fromFile(filePath)
         var linesFromFile = tsvFile.getLines
         var tsvHeader: Option[List[String]] = Some(linesFromFile.next().split("\t").toList)
         var tsvContent: Option[List[List[String]]] = Some(getLineContentsFromLines(linesFromFile, 4))
         tsvFile.close()
         var isColumnNameInSettingsArray: ArrayBuffer[Boolean] = new ArrayBuffer()
         for (headerName <- tsvHeader.get) {
            isColumnNameInSettingsArray.+=(PreferredHeaderDAO.isPreferredHeaderNameForUser(headerName, rs.request.session.get("username").get)(rs.dbSession))
         }
         
         val releaseName = rs.request.session.get("releaseName").get
         if (TSVFileDAO.tsvFileExists(fileName, fileType)(rs.dbSession)) {
            if (!TSVFileDAO.tsvFileExistsInRelease(releaseName, fileName)(rs.dbSession)) {
               TSVFileDAO.addTSVFileToRelease(releaseName, fileName, fileType)(rs.dbSession)
            }
         } else {
            TSVFileDAO.createTSVFile(releaseName, fileName, filePath, fileType)(rs.dbSession)
         }
         
         if (rs.request.session.get("uploadErrorMessage").isDefined) {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent, isColumnNameInSettingsArray.toArray, false)).withSession(rs.request.session - "uploadErrorMessage" + ("tsvFileName" -> fileName))
         } else {
            Ok(views.html.sampleSelection(tsvHeader, tsvContent, isColumnNameInSettingsArray.toArray)).withSession(rs.request.session + ("tsvFileName" -> fileName))
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
            var releaseName = rs.request.session.get("releaseName").get
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
                  if (!SampleDAO.sampleExistsInFile(tsvFileName, releaseName, sampleName)(rs.dbSession)) {
                     if (SampleDAO.sampleExists(sampleName)(rs.dbSession)) {
                        TSVFileSampleLinkDAO.createTSVFileSampleLink(tsvFileName, releaseName, sampleName)(rs.dbSession)
                     } else {
                        SampleDAO.createSample(tsvFileName, releaseName, sampleName)(rs.dbSession)
                     }
                  }
                  for (sampleFileName <- SampleFileDAO.getAllSampleFileNames()(rs.dbSession)) {
                     if (sampleFileName.indexOf(sampleName) != -1 && !SampleSampleFileLinkDAO.linkExists(sampleName, sampleFileName)(rs.dbSession)) {
                        SampleSampleFileLinkDAO.createLink(sampleName, sampleFileName)(rs.dbSession)
                     }
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

   def fileContentsAreEqual(file1: File, file2: File): Boolean = {
      var scanner1 = new Scanner(file1.toPath(), "UTF-8")
      var scanner2 = new Scanner(file2.toPath(), "UTF-8")
      while (scanner1.hasNextLine() && scanner2.hasNextLine()) {
         if (!scanner1.nextLine().equals(scanner2.nextLine())) {
            scanner1.close()
            scanner2.close()
            return false
         }
      }
      if (scanner1.hasNextLine() != scanner2.hasNextLine()) {
         scanner1.close()
         scanner2.close()
         return false
      }
      scanner1.close()
      scanner2.close()
      return true
   }

   val sampleTypeSelectionForm = Form("sampleTypes" -> text)
   
   val headerSelectionForm = Form("value" -> list(boolean))
}