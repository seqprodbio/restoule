package controllers.preferences

import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Controller
import models.preferences.LocalDirectory
import play.api.i18n.Messages

object LocalDirectories extends Controller {

  def list = Action { implicit request =>
    val localDirectories = LocalDirectory.findAll
    Ok(views.html.preferences.localdirectory(localDirectories))
  }

}