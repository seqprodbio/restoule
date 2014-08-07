package models.persistance

import models.SampleLIMSInfo
import models.SampleLIMSInfoTable

import play.api.db.slick.Config.driver.simple._

object SampleLIMSInfoDAO {

   val sampleLimsInfo = TableQuery[SampleLIMSInfoTable]

   def getAllSampleLimsInfo() = { implicit session: Session =>
      sampleLimsInfo.list
   }

   def getSampleLimsInfoById(id: Int) = { implicit session: Session =>
      sampleLimsInfo.filter(s => s.id === id).firstOption
   }

   def getSampleLimsInfoByLibraryName(libraryName: String) = { implicit session: Session =>
      sampleLimsInfo.filter(s => s.libraryName === libraryName).firstOption
   }

   def createSampleLimsInfo(libraryName: String, donor: String, libraryStrategy: String, librarySource: String, librarySelection: String) = { implicit session: Session =>
      var newSampleLimsInfo = SampleLIMSInfo(None, libraryName, donor, libraryStrategy, librarySource, librarySelection)
      sampleLimsInfo.insert(newSampleLimsInfo)
   }

}