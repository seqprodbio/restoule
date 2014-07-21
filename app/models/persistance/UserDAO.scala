package models.persistance

import models.User
import models.UserTable
import play.api.db.slick.Config.driver.simple._

object UserDAO {

   val users = TableQuery[UserTable]

   def existsUserWithName(username: String) = { implicit session: Session =>
      if (users.filter(u => u.username === username).list.length > 0) {
         true
      } else {
         false
      }
   }

   def createUserWithName(username: String) = { implicit session: Session =>
      val newUser = new User(None, username, new java.sql.Timestamp(System.currentTimeMillis()))
      users.insert(newUser)
   }

   def getIdFromName(username: String) = { implicit session: Session =>
      users.filter(u => u.username === username).map(u => u.id).first
   }
}