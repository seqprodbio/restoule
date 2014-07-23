package models.persistance

import models.Release
import models.ReleaseInfo
import models.ReleaseTable

import play.api.db.slick.Config.driver.simple._

object ReleaseDAO {

   val releases = TableQuery[ReleaseTable]

   def getReleaseNames() = { implicit session: Session =>
      releases.map(r => r.name).list
   }

   def releaseNameExists(releaseName: String) = { implicit session: Session =>
      if (releases.filter(r => r.name === releaseName).list.length > 0) {
         true;
      } else {
         false;
      }
   }

   def getReleaseIdFromName(releaseName: String) = { implicit session: Session =>
      releases.filter(r => r.name === releaseName).map(r => r.id).first
   }

   def getReleaseInfoFromId(releaseId: Int) = { implicit session: Session =>
      var release = releases.filter(r => r.id === releaseId).first
      var releaseInfo = new ReleaseInfo(release.studyName, release.studyAbstract)
      releaseInfo
   }

   def createRelease(releaseName: String, username: String) = { implicit session: Session =>
      val newRelease = new Release(None, releaseName, UserDAO.getIdFromName(username)(session), "", "", new java.sql.Timestamp(System.currentTimeMillis()))
      releases.insert(newRelease)
   }

   def updateReleaseInfoById(releaseId: Int, releaseInfo: ReleaseInfo) = { implicit session: Session =>
      val studyNameQuery = for { r <- releases if r.id === releaseId } yield r.studyName
      studyNameQuery.update(releaseInfo.studyName)
      val studyNameUpdateStatement = studyNameQuery.updateStatement
      val studyNameUpdateInvoker = studyNameQuery.updateInvoker

      val studyAbstractQuery = for { r <- releases if r.id === releaseId } yield r.studyAbstract
      studyAbstractQuery.update(releaseInfo.studyAbstract)
      val studyAbstractUpdateStatement = studyAbstractQuery.updateStatement
      val studyAbstractUpdateInvoker = studyAbstractQuery.updateInvoker
   }
}