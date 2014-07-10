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
      if (getTSVIdFromFileName(tsvFileName)(session).isDefined) {
         ReleaseTSVFileLinkDAO.tsvFileExistsInRelease(releaseId, getTSVIdFromFileName(tsvFileName)(session).get)(session)
      } else {
         false
      }
   }

   def createTSVFile(releaseName: String, fileName: String, path: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val newTSVFile = new TSVFile(None, fileName, path, new java.sql.Date(System.currentTimeMillis()))
      tsvFiles.insert(newTSVFile)
      ReleaseTSVFileLinkDAO.createLink(releaseId, getTSVIdFromFileName(fileName)(session).get)(session)
   }

   def addTSVFileToRelease(releaseName: String, fileName: String) = { implicit session: Session =>
      val releaseId = ReleaseDAO.getReleaseIdFromName(releaseName)(session)
      val tsvFileId = getTSVIdFromFileName(fileName)(session).get
      ReleaseTSVFileLinkDAO.createLink(releaseId, tsvFileId)(session)
   }

   def getTSVIdFromFileName(filename: String) = { implicit session: Session =>
      tsvFiles.filter(t => t.name === filename).map(t => t.id).firstOption
   }

   def tsvFileExists(filename: String) = { implicit session: Session =>
      if (getTSVIdFromFileName(filename)(session).isDefined) {
         true
      } else {
         false
      }
   }

}
