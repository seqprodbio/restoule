package models

import play.api.db.slick.Config.driver.simple._

case class Release(id: Option[Int], name: String, userId: Int, studyName: String, studyAbstract: String, created: java.sql.Timestamp)

class ReleaseTable(tag: Tag) extends Table[Release](tag, "release") {
   val users = TableQuery[UserTable]

   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
   def name = column[String]("name", O.NotNull)
   def userId = column[Int]("user_id", O.NotNull)
   def studyName = column[String]("study_name", O.NotNull)
   def studyAbstract = column[String]("study_abstract", O.NotNull)
   def created = column[java.sql.Timestamp]("created_tstmp", O.NotNull)

   def * = (id.?, name, userId, studyName, studyAbstract, created) <> (Release.tupled, Release.unapply _)

   def user = foreignKey("user_FK", userId, users)(_.id)
}