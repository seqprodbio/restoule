package models;

import play.api.db.slick.Config.driver.simple._

case class SampleFile(id: Option[Int], fileName: String, path: String, origin: String, swid: Int, sample: String, library: String, sequencerRunName: String, lane: Int, barcode: String, read: Int, createdInDB: java.sql.Timestamp)

class SampleFileTable(tag: Tag) extends Table[SampleFile](tag, "sample_file") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def fileName = column[String]("filename", O.NotNull)
   def path = column[String]("path", O.NotNull)
   def origin = column[String]("origin", O.NotNull)
   def swid = column[Int]("swid", O.NotNull)
   def sample = column[String]("sample", O.NotNull)
   def library = column[String]("library", O.NotNull)
   def sequencerRunName = column[String]("sequencer_run", O.NotNull)
   def lane = column[Int]("lane", O.NotNull)
   def barcode = column[String]("barcode", O.NotNull)
   def read = column[Int]("read", O.NotNull)
   def createdInDB = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, fileName, path, origin, swid, sample, library, sequencerRunName, lane, barcode, read, createdInDB) <> (SampleFile.tupled, SampleFile.unapply)
}