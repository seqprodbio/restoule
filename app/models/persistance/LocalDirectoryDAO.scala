package models.persistance

import models.LocalDirectory
import models.LocalDirectoryTable

import play.api.db.slick.Config.driver.simple._

object LocalDirectoryDAO {

   val localDirectories = TableQuery[LocalDirectoryTable]

   def getAllDirectories() = { implicit session: Session =>
      localDirectories.list
   }

   def createLocalDirectory(path: String) = { implicit session: Session =>
      localDirectories.insert(new LocalDirectory(None, path, new java.sql.Date(System.currentTimeMillis())))
   }
}