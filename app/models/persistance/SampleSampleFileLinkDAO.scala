package models.persistance

import models.SampleSampleFileLink
import models.SampleSampleFileLinkTable

import play.api.db.slick.Config.driver.simple._

object SampleSampleFileLinkDAO {

   def sampleSampleFileLinks = TableQuery[SampleSampleFileLinkTable]

   def doesLinkExist(sampleName: String, sampleFileName: String) = { implicit session: Session =>
      val sampleId = SampleDAO.getIdFromSampleName(sampleName)(session)
      val sampleFileId = SampleFileDAO.getIdFromSampleFileName(sampleFileName)(session)
      if (sampleSampleFileLinks.filter(s => s.sampleId === sampleId && s.sampleFileId === sampleFileId).firstOption.isDefined) {
         true
      } else {
         false
      }
   }

   def getFileIdsFromSampleName(sampleName: String) = { implicit session: Session =>
      val sampleId = SampleDAO.getIdFromSampleName(sampleName)(session)
      sampleSampleFileLinks.filter(s => s.sampleId === sampleId).map(s => s.sampleFileId).list
   }

   def createLink(sampleName: String, sampleFileName: String) = { implicit session: Session =>
      val sampleId = SampleDAO.getIdFromSampleName(sampleName)(session)
      val sampleFileId = SampleFileDAO.getIdFromSampleFileName(sampleFileName)(session)
      sampleSampleFileLinks.insert(new SampleSampleFileLink(None, sampleId, sampleFileId, new java.sql.Timestamp(System.currentTimeMillis())))
   }

   def deleteAll() = { implicit session: Session =>
      sampleSampleFileLinks.delete
   }
}