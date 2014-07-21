package models

import play.api.db.slick.Config.driver.simple._

case class TSVFile(id: Option[Int], name: String, path: String, created: java.sql.Timestamp)

class TSVFileTable(tag: Tag) extends Table[TSVFile](tag, "tsv_file") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def name = column[String]("name", O.NotNull)
   def path = column[String]("path", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, name, path, created) <> (TSVFile.tupled, TSVFile.unapply _)
}