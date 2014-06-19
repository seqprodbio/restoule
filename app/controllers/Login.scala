package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Session
import play.api.data.{ Form }
import play.api.data.Forms._
import models.LoginInfo

object Login extends Controller {
  
  def showLogin = Action { implicit request =>
    val form = if(request.session.get("errorMessage").isDefined && request.session.get("errorMessage").get.equals("Not authorized")){
      loginForm.bind(request.session.data)
    } else {
      loginForm
    }
    Ok(views.html.loginPage(form))
  }
  
  def processForm = Action { implicit request =>
  	loginForm.bindFromRequest().fold(
  			formWithErrors => Redirect(routes.Login.showLogin()).withSession("errorMessage" -> "You must provide a username"),
  			login => {
  				if(authenticateLogin(login)){
  				  Redirect(routes.Application.entrance()).withSession("loginTime" -> "Just a few seconds ago")
  				} else {
  				  Redirect(routes.Login.showLogin()).withSession(("errorMessage" -> "Not authorized"), ("username" -> login.username), ("password" -> ""))
  				}
  			})
  }
  
  def authenticateLogin (login : LoginInfo) : Boolean = {
    return (login.username.equals("mnerandzic"))
  }
  
  val loginForm = Form(mapping(
      "username" -> nonEmptyText,
      "password" -> text)(LoginInfo.apply)(LoginInfo.unapply))

}