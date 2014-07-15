package models

import play.api.db.slick.Config.driver.simple._

case class Release(id: Option[Int], name: String, userId: Int, created: java.sql.Date)

class ReleaseTable(tag: Tag) extends Table[Release](tag, "release") {
   val users = TableQuery[UserTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def name = column[String]("name", O.NotNull)
   def userId = column[Int]("user_id", O.NotNull)
   def created = column[java.sql.Date]("created_date", O.NotNull)

   def * = (id.?, name, userId, created) <> (Release.tupled, Release.unapply _)

   def user = foreignKey("user_FK", userId, users)(_.id)
}