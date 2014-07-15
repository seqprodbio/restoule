package models.persistance

import models.Sample
import models.SampleTable

import play.api.db.slick.Config.driver.simple._

object SampleDAO {

   val samples = TableQuery[SampleTable]

   def getSamplesFromFile(filename: String) = { implicit session: Session =>
      val fileId = TSVFileDAO.getTSVIdFromFileName(filename)(session)
      samples.filter(s => s.tsvFileId === fileId).list
   }

   def createSample(tsvFileName: String, name: String, fs: String, box: String, run: String, barcode: String, librarySource: String, libraryStrategy: String, complete: Boolean) = { implicit session: Session =>
      val tsvFileId = TSVFileDAO.getTSVIdFromFileName(tsvFileName)(session).get
      val newSample = new Sample(None, tsvFileId, name, fs, box, run, barcode, librarySource, libraryStrategy, complete, new java.sql.Date(System.currentTimeMillis()))
      samples.insert(newSample)
   }

   def sampleExists(fileName: String, sampleName: String) = { implicit session: Session =>
      val fileId = TSVFileDAO.getTSVIdFromFileName(fileName)(session).get
      if (samples.filter(s => s.name === sampleName && s.tsvFileId === fileId).firstOption.isDefined) {
         true
      } else {
         false
      }
   }
}