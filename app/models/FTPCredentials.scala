package models

import play.api.db.slick.Config.driver.simple._

case class FTPCredentials(id: Option[Int], ftpSite: String, userName: String, password: String, created: java.sql.Date)

class FTPCredentialsTable(tag: Tag) extends Table[FTPCredentials](tag, "ftp_credentials") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def ftpSite = column[String]("ftp_site", O.NotNull)
   def userName = column[String]("username", O.NotNull)
   def password = column[String]("password", O.NotNull)
   def created = column[java.sql.Date]("created_date", O.NotNull)

   def * = (id.?, ftpSite, userName, password, created) <> (FTPCredentials.tupled, FTPCredentials.unapply)
}