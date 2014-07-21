package models.persistance

import models.SampleFile
import models.SampleFileTable

import play.api.db.slick.Config.driver.simple._

object SampleFileDAO {

   val sampleFiles = TableQuery[SampleFileTable]

   def getAllSampleFiles() = { implicit session: Session =>
      sampleFiles.list
   }

   def getAllSampleFileNames() = { implicit session: Session =>
      sampleFiles.map(s => s.fileName).list
   }

   def getAllFTPSampleFiles() = { implicit session: Session =>
      sampleFiles.filter(s => s.origin === "ftp").list
   }

   def getAllLocalSampleFiles() = { implicit session: Session =>
      sampleFiles.filter(s => s.origin === "local").list
   }

   def doesSampleFileExist(path: String) = { implicit session: Session =>
      if (sampleFiles.filter(s => s.path === path).firstOption.isDefined) {
         true
      } else {
         false
      }
   }

   def getIdFromSampleFileName(fileName: String) = { implicit session: Session =>
      sampleFiles.filter(s => s.fileName === fileName).map(s => s.id).first
   }

   def createSampleFile(path: String, origin: String) = { implicit session: Session =>
      var pathParts = path.split("/")
      var fileName = pathParts(pathParts.length - 1)
      var newSampleFile = new SampleFile(None, fileName, path, origin, new java.sql.Timestamp(System.currentTimeMillis()))
      sampleFiles.insert(newSampleFile)
   }

   def deleteAll() = { implicit session: Session =>
      sampleFiles.delete
   }
}