package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Session

object Application extends Controller {

  def entrance = Action { request =>
    if(request.session.get("loginTime").isDefined){
      Redirect(routes.Application.mainPage())
    }else{
      Redirect(routes.Login.showLogin())
    }
  }
  
  def mainPage = Action { request =>
  	Ok(views.html.mainPage())
  }

}