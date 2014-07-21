package models.persistance

import play.api.db.slick.Config.driver.simple._
import models.Sample
import models.TSVFileSampleLink
import models.TSVFileSampleLinkTable
import scala.collection.mutable.ListBuffer

object TSVFileSampleLinkDAO {

   val tsvFileSampleLinks = TableQuery[TSVFileSampleLinkTable]

   def getAllSamplesInTSVFile(tsvFileName: String) = { implicit session: Session =>
      val tsvId = TSVFileDAO.getTSVIdFromFileName(tsvFileName)(session).get
      val sampleIds = tsvFileSampleLinks.filter(l => l.tsvFileId === tsvId).map(l => l.sampleId).list
      var samplesList = new ListBuffer[Sample]()
      for (sampleId <- sampleIds) {
         samplesList += SampleDAO.getSampleFromId(sampleId)(session)
      }
      samplesList.toList
   }

   def sampleExistsInFile(fileId: Int, sampleId: Int) = { implicit session: Session =>
      if (tsvFileSampleLinks.filter(l => l.sampleId === sampleId && l.tsvFileId === fileId).firstOption.isDefined) {
         true
      } else {
         false
      }
   }

   def createTSVFileSampleLink(tsvFileName: String, sampleName: String) = { implicit session: Session =>
      val tsvId = TSVFileDAO.getTSVIdFromFileName(tsvFileName)(session).get
      val sampleId = SampleDAO.getIdFromSampleName(sampleName)(session)
      tsvFileSampleLinks.insert(new TSVFileSampleLink(None, tsvId, sampleId, new java.sql.Timestamp(System.currentTimeMillis())))
   }
}