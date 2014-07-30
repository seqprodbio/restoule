package models

import play.api.db.slick.Config.driver.simple._

case class SampleSampleFileLink(id: Option[Int], sampleId: Int, sampleFileId: Int, created: java.sql.Timestamp)

class SampleSampleFileLinkTable(tag: Tag) extends Table[SampleSampleFileLink](tag, "sample_sample_file_link") {
   val samples = TableQuery[SampleTable]
   val sampleFiles = TableQuery[SampleFileTable]
   
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def sampleId = column[Int]("sample_id", O.NotNull)
   def sampleFileId = column[Int]("sample_file_id", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, sampleId, sampleFileId, created) <> (SampleSampleFileLink.tupled, SampleSampleFileLink.unapply)

   def sample = foreignKey("SAMPLE_FK", sampleId, samples)(_.id)
   def sampleFile = foreignKey("SAMPLE_FILE_FK", sampleFileId, sampleFiles)(_.id)
}
