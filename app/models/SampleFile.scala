package models;

import play.api.db.slick.Config.driver.simple._

case class SampleFile(id: Option[Int], fileName: String, path: String, origin: String, createdInDB: java.sql.Timestamp)

class SampleFileTable(tag: Tag) extends Table[SampleFile](tag, "sample_file") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def fileName = column[String]("filename", O.NotNull)
   def path = column[String]("path", O.NotNull)
   def origin = column[String]("origin", O.NotNull)
   def createdInDB = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, fileName, path, origin, createdInDB) <> (SampleFile.tupled, SampleFile.unapply)
}