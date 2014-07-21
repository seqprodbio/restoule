package models.persistance

import models.Release
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

   def createRelease(releaseName: String, username: String) = { implicit session: Session =>
      val newRelease = new Release(None, releaseName, UserDAO.getIdFromName(username)(session), new java.sql.Timestamp(System.currentTimeMillis()))
      releases.insert(newRelease)
   }

   def getReleaseIdFromName(releaseName: String) = { implicit session: Session =>
      releases.filter(r => r.name === releaseName).map(r => r.id).first
   }
}