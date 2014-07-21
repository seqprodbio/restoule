package models.persistance

import models.FTPCredentials
import models.FTPCredentialsTable

import play.api.db.slick.Config.driver.simple._

object FTPCredentialsDAO {
   def ftpCredentials = TableQuery[FTPCredentialsTable]

   def getAllFTPCredentials() = { implicit session: Session =>
      ftpCredentials.list
   }

   def createFTPCredentials(ftpSite: String, userName: String, password: String) = { implicit session: Session =>
      ftpCredentials.insert(new FTPCredentials(None, ftpSite, userName, password, new java.sql.Timestamp(System.currentTimeMillis())))
   }
}