package models

import play.api.db.slick.Config.driver.simple._

case class TSVFileSampleLink(id: Option[Int], tsvFileId: Int, sampleId: Int, created: java.sql.Timestamp)

class TSVFileSampleLinkTable(tag: Tag) extends Table[TSVFileSampleLink](tag, "tsv_file_sample_link") {
   val tsvFiles = TableQuery[TSVFileTable]
   val samples = TableQuery[SampleTable]
   
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def tsvFileId = column[Int]("tsv_file_id", O.NotNull)
   def sampleId = column[Int]("sample_id", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, tsvFileId, sampleId, created) <> (TSVFileSampleLink.tupled, TSVFileSampleLink.unapply)

   def tsvFile = foreignKey("TSV_FILE_FK", tsvFileId, tsvFiles)(_.id)
   def sample = foreignKey("SAMPLE_FK", sampleId, samples)(_.id)
}