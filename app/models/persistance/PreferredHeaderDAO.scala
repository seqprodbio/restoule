package models.persistance

import models.PreferredHeader
import models.PreferredHeaderTable

import play.api.db.slick.Config.driver.simple._

object PreferredHeaderDAO {

   val preferredHeaders = TableQuery[PreferredHeaderTable]

   def isPreferredHeaderNameForUser(preferredHeaderName: String, userName: String) = { implicit session: Session =>
      val userId = UserDAO.getIdFromName(userName)(session)
      val preferredHeader = preferredHeaders.filter(p => p.userId === userId && p.name === preferredHeaderName).firstOption
      if (preferredHeader.isDefined) {
         true
      } else {
         false
      }
   }

   def createPreferredHeader(userName: String, headerName: String) = { implicit session: Session =>
      val userId = UserDAO.getIdFromName(userName)(session)
      var newPreferredHeader = new PreferredHeader(None, userId, headerName, new java.sql.Date(System.currentTimeMillis()))
      preferredHeaders.insert(newPreferredHeader)
   }
}