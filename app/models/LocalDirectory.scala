package models;

import play.api.db.slick.Config.driver.simple._

case class LocalDirectory(id: Option[Int], path: String, created: java.sql.Timestamp)

class LocalDirectoryTable(tag: Tag) extends Table[LocalDirectory](tag, "local_dir") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def path = column[String]("path", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, path, created) <> (LocalDirectory.tupled, LocalDirectory.unapply)
}