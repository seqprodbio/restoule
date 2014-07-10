package models;

import play.api.db.slick.Config.driver.simple._

case class Sample(id: Option[Int], tsvFileId: Int, name: String, fs: String, box: String, run: String, barcode: String, librarySource: String, libraryStrategy: String, complete: Boolean, created: java.sql.Date)

class SampleTable(tag: Tag) extends Table[Sample](tag, "sample") {
   val tsvFiles = TableQuery[TSVFileTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def tsvFileId = column[Int]("tsv_file_id", O.NotNull)
   def name = column[String]("name", O.NotNull)
   def fs = column[String]("fs", O.NotNull)
   def box = column[String]("box", O.NotNull)
   def run = column[String]("run", O.NotNull)
   def barcode = column[String]("barcode", O.NotNull)
   def librarySource = column[String]("library_source", O.NotNull)
   def libraryStrategy = column[String]("library_strategy", O.NotNull)
   def complete = column[Boolean]("complete", O.NotNull)
   def created = column[java.sql.Date]("created_date", O.NotNull)

   def * = (id.?, tsvFileId, name, fs, box, run, barcode, librarySource, libraryStrategy, complete, created) <> (Sample.tupled, Sample.unapply)

   def tsvFile = foreignKey("TSVFILE_FK", tsvFileId, tsvFiles)(_.id)
}
