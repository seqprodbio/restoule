package models;

import play.api.db.slick.Config.driver.simple._

case class Sample(id: Option[Int], name: String, complete: Boolean, created: java.sql.Timestamp)

class SampleTable(tag: Tag) extends Table[Sample](tag, "sample") {
   val tsvFiles = TableQuery[TSVFileTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def name = column[String]("name", O.NotNull)
   def complete = column[Boolean]("complete", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, name, complete, created) <> (Sample.tupled, Sample.unapply)
}
