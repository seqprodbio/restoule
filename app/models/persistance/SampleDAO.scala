package models.persistance

import models.Sample
import models.SampleTable

import play.api.db.slick.Config.driver.simple._

object SampleDAO {

   val samples = TableQuery[SampleTable]

   def getSampleFromId(id: Int) = { implicit session: Session =>
      samples.filter(s => s.id === id).first
   }

   def createSample(tsvFileName: String, releaseName: String, name: String) = { implicit session: Session =>
      val newSample = new Sample(None, name, new java.sql.Timestamp(System.currentTimeMillis()))
      samples.insert(newSample)
      TSVFileSampleLinkDAO.createTSVFileSampleLink(tsvFileName, releaseName, name)(session)
   }

   def getSampleFromSampleName(sampleName: String) = { implicit session: Session =>
      samples.filter(s => s.name === sampleName).first
   }

   def getIdFromSampleName(sampleName: String) = { implicit session: Session =>
      getSampleFromSampleName(sampleName)(session).id.get
   }

   def getAllSampleNames() = { implicit session: Session =>
      samples.map(s => s.name).list
   }

   def sampleExists(sampleName: String) = { implicit session: Session =>
      if (samples.filter(s => s.name === sampleName).firstOption.isDefined) {
         true
      } else {
         false
      }
   }

   def sampleExistsInFile(fileName: String, releaseName: String, sampleName: String) = { implicit session: Session =>
      val fileId = TSVFileDAO.getTSVIdFromFileNameAndReleaseName(fileName, releaseName)(session).get
      if (samples.filter(s => s.name === sampleName).firstOption.isDefined) {
         if (TSVFileSampleLinkDAO.sampleExistsInFile(fileId, getIdFromSampleName(sampleName)(session))(session)) {
            true
         } else {
            false
         }
      } else {
         false
      }
   }

   def hasCompleteSampleFile(sample: Sample) = { implicit session: Session =>
      var hasComplete = false
      var fileIds = SampleSampleFileLinkDAO.getFileIdsFromSampleName(sample.name)(session)
      for (fileId <- fileIds) {
         if (SampleFileDAO.isSampleFileComplete(SampleFileDAO.getSampleFileFromId(fileId)(session))(session)) {
            hasComplete = true
         }
      }
      hasComplete
   }
}