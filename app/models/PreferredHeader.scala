package models

import play.api.db.slick.Config.driver.simple._

case class PreferredHeader(id: Option[Int], userId: Int, name: String, created: java.sql.Date)

class PreferredHeaderTable(tag: Tag) extends Table[PreferredHeader](tag, "pref_header") {
   val users = TableQuery[UserTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def userId = column[Int]("user_id", O.NotNull)
   def name = column[String]("name", O.NotNull)
   def created = column[java.sql.Date]("created_date", O.NotNull)

   def * = (id.?, userId, name, created) <> (PreferredHeader.tupled, PreferredHeader.unapply _)

   def user = foreignKey("USER_FK", userId, users)(_.id)
}