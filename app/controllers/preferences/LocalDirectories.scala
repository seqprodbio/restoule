package controllers.preferences

import models.preferences.LocalDirectory
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.number
import play.api.data.Forms._
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Flash

object LocalDirectories extends Controller {

  private val localDirectoryForm: Form[LocalDirectory] = Form(
    mapping(
      "id" -> longNumber,
      "dir" -> nonEmptyText,
      "size" -> number)(LocalDirectory.apply)(LocalDirectory.unapply))

  def list = Action { implicit request =>
    val localDirectories = LocalDirectory.findAll
    Ok(views.html.preferences.localdirectory(localDirectories, localDirectoryForm))
  }

  def save = Action { implicit request =>
    val newProductForm = localDirectoryForm.bindFromRequest()
    newProductForm.fold(
      hasErrors = { form =>
        Redirect(routes.LocalDirectories.list())
      },
      success = { newProduct =>
        LocalDirectory.add(newProduct)
        Redirect(routes.LocalDirectories.list())
      })

  }

  def newProduct = Action { implicit request =>
    val form = if (request.flash.get("error").isDefined)
      localDirectoryForm.bind(request.flash.data)
    else
      localDirectoryForm
    val localDirectories = LocalDirectory.findAll
    Ok(views.html.preferences.localdirectory(localDirectories, localDirectoryForm))
  }

}