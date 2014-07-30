package models.persistance

import scala.collection.mutable.ListBuffer

import models.TSVFile
import models.TSVFileTable
import models.ReleaseTSVFileLink
import models.ReleaseTSVFileLinkTable

import play.api.db.slick.Config.driver.simple._

object TSVFileDAO {

   val tsvFiles = TableQuery[TSVFileTable]
   val releaseTSVLinks = TableQuery[ReleaseTSVFileLinkTable]

   def getTSVFilesFromReleaseName(releaseName: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      ReleaseTSVFileLinkDAO.getFilesFromReleaseId(releaseId)(session)
   }

   def getTSVFileNamesFromReleaseName(releaseName: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val files = ReleaseTSVFileLinkDAO.getFilesFromReleaseId(releaseId)(session)
      var fileNames = new ListBuffer[String]()
      for (file <- files) {
         fileNames.append(file.name)
      }
      fileNames.toList
   }

   def tsvFileExistsInRelease(releaseName: String, tsvFileName: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      if (getTSVIdFromFileNameAndReleaseName(tsvFileName, releaseName)(session).isDefined) {
         ReleaseTSVFileLinkDAO.tsvFileExistsInRelease(releaseId, getTSVIdFromFileNameAndReleaseName(tsvFileName, releaseName)(session).get)(session)
      } else {
         false
      }
   }

   def createTSVFile(releaseName: String, fileName: String, path: String, fileType: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val newTSVFile = new TSVFile(None, fileName, path, fileType, new java.sql.Timestamp(System.currentTimeMillis()))
      tsvFiles.insert(newTSVFile)
      ReleaseTSVFileLinkDAO.createLink(releaseId, getTSVIdFromFileNameAndSampleFileTypes(fileName, fileType)(session).get)(session)
   }

   def addTSVFileToRelease(releaseName: String, fileName: String, fileType: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val tsvFileId = getTSVIdFromFileNameAndSampleFileTypes(fileName, fileType)(session).get
      ReleaseTSVFileLinkDAO.createLink(releaseId, tsvFileId)(session)
   }

   def getTSVIdFromFileNameAndReleaseName(fileName: String, releaseName: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val files = ReleaseTSVFileLinkDAO.getFilesFromReleaseId(releaseId)(session)
      var id: Option[Int] = None
      for(file <- files){
        if(file.name.equals(fileName)){
          id = file.id
        }
      }
      id
   }
   
   def getTSVIdFromFileNameAndSampleFileTypes(fileName: String, sampleFileType: String) = { implicit session: Session =>
      tsvFiles.filter(t => t.name === fileName && t.fileType === sampleFileType).map(t => t.id).firstOption
   }
   
   def getFileTypeFromFileNameAndReleaseName(fileName: String, releaseName: String) = { implicit session: Session =>
      var tsvId = getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(session)
      tsvFiles.filter(t => t.id === tsvId).map(t => t.fileType).first
   }

   def getFileNameFromId(fileId: Int) = { implicit session: Session =>
      tsvFiles.filter(t => t.id === fileId).map(t => t.name).first
   }

   def tsvFileExists(fileName: String, fileTypes: String) = { implicit session: Session =>
      if (getTSVIdFromFileNameAndSampleFileTypes(fileName, fileTypes)(session).isDefined) {
         true
      } else {
         false
      }
   }
   
   def updateFileTypeFromId(fileId: Int, fileType: String) = { implicit session: Session =>
     val fileTypeQuery = for{ t <- tsvFiles if t.id === fileId} yield t.fileType
     fileTypeQuery.update(fileType)
     val fileTypeUpdateStatement = fileTypeQuery.updateStatement
     val fileTypeUpdateInvoker = fileTypeQuery.updateInvoker
   }

}
