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

   //Note thatI assume that each library name in the table is unique so if that is changed, there may be an issue with everything using this function
   def getSampleLimsInfoByLibraryName(libraryName: String) = { implicit session: Session =>
      sampleLimsInfo.filter(s => s.libraryName === libraryName).firstOption
   }

   def createSampleLimsInfo(libraryName: String, donor: String, libraryStrategy: Option[String], librarySource: Option[String], librarySelection: Option[String]) = { implicit session: Session =>
      var actualStrategy: String = libraryStrategy.getOrElse("")
      var actualSource: String = librarySource.getOrElse("")
      var actualSelection: String = librarySelection.getOrElse("")
      var newSampleLimsInfo = SampleLIMSInfo(None, libraryName, donor, actualStrategy, actualSource, actualSelection, new java.sql.Timestamp(System.currentTimeMillis()))
      sampleLimsInfo.insert(newSampleLimsInfo)
   }

   def isComplete(id: Int) = { implicit session: Session =>
      val sampleLimsInfo = getSampleLimsInfoById(id)(session).get
      if (!sampleLimsInfo.libraryName.equals("") && !sampleLimsInfo.donor.equals("") && !sampleLimsInfo.libraryStrategy.equals("") && !sampleLimsInfo.librarySource.equals("") && !sampleLimsInfo.librarySelection.equals("")) {
         true
      } else {
         false
      }
   }

}