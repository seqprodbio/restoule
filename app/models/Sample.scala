package models;

import play.api.db.slick.Config.driver.simple._

case class Sample(id: Option[Int], name: String, fs: String, box: String, run: String, barcode: String, librarySource: String, libraryStrategy: String, complete: Boolean, created: java.sql.Timestamp)

class SampleTable(tag: Tag) extends Table[Sample](tag, "sample") {
   val tsvFiles = TableQuery[TSVFileTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def name = column[String]("name", O.NotNull)
   def fs = column[String]("fs", O.NotNull)
   def box = column[String]("box", O.NotNull)
   def run = column[String]("run", O.NotNull)
   def barcode = column[String]("barcode", O.NotNull)
   def librarySource = column[String]("library_source", O.NotNull)
   def libraryStrategy = column[String]("library_strategy", O.NotNull)
   def complete = column[Boolean]("complete", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, name, fs, box, run, barcode, librarySource, libraryStrategy, complete, created) <> (Sample.tupled, Sample.unapply)
}
