package models

import play.api.db.slick.Config.driver.simple._

case class User(id: Option[Int], username: String, created: java.sql.Date)

class UserTable(tag: Tag) extends Table[User](tag, "users") {
   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def username = column[String]("username", O.NotNull)
   def created = column[java.sql.Date]("created_date", O.NotNull)

   def * = (id.?, username, created) <> (User.tupled, User.unapply _)
}