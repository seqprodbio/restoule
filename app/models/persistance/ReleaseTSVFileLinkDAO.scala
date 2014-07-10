package models.persistance

import models.ReleaseTSVFileLink
import models.ReleaseTSVFileLinkTable

import play.api.db.slick.Config.driver.simple._

object ReleaseTSVFileLinkDAO {

   val releaseTSVLinks = TableQuery[ReleaseTSVFileLinkTable]

   def getFileIdsFromReleaseId(releaseId: Int) = { implicit session: Session =>
      releaseTSVLinks.filter(l => l.releaseId === releaseId).map(l => l.tsvFileId).list
   }

   def getFilesFromReleaseId(releaseId: Int) = { implicit session: Session =>
      val query = for {
         r <- releaseTSVLinks
         t <- r.tsvFile if r.releaseId === releaseId
      } yield (t)
      query.list
   }

   def tsvFileExistsInRelease(releaseId: Int, tsvFileId: Int) = { implicit session: Session =>
      if (releaseTSVLinks.filter(l => l.releaseId === releaseId && l.tsvFileId === tsvFileId).list.length > 0) {
         true
      } else {
         false
      }
   }

   def getReleaseIdsFromFileId(fileId: Int) = { implicit session: Session =>
      releaseTSVLinks.filter(l => l.tsvFileId === fileId).map(l => l.releaseId).list
   }

   def createLink(releaseId: Int, fileId: Int) = { implicit session: Session =>
      val newReleaseTSVFileLink = new ReleaseTSVFileLink(None, releaseId, fileId, new java.sql.Date(System.currentTimeMillis()))
      releaseTSVLinks.insert(newReleaseTSVFileLink)
   }
}