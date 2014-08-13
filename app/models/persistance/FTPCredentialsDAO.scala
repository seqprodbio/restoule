package models.persistance

import models.FTPCredentials
import models.FTPCredentialsTable

import play.api.db.slick.Config.driver.simple._

object FTPCredentialsDAO {
   def ftpCredentials = TableQuery[FTPCredentialsTable]

   def getAllFTPCredentials() = { implicit session: Session =>
      ftpCredentials.list
   }

   //This assumes that the ftpSite is unique, if this is not the case, change all code that uses this function
   def getFTPCredentialsFromSite(ftpSite: String) = { implicit session: Session =>
      ftpCredentials.filter(f => f.ftpSite === ftpSite).first
   }

   def createFTPCredentials(ftpSite: String, userName: String, password: String) = { implicit session: Session =>
      ftpCredentials.insert(new FTPCredentials(None, ftpSite, userName, password, new java.sql.Timestamp(System.currentTimeMillis())))
   }
}