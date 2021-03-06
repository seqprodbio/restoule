import scala.concurrent._
import scala.collection.immutable.StringOps
import play.api._
import play.api.mvc._
import play.api.mvc.RequestHeader
import play.api.{ GlobalSettings, Logger }

object Global extends GlobalSettings {
   override def onRouteRequest(request: RequestHeader) = {
      println("recieved request for [%s]" format request)
      if (request.method.equals("GET") && !request.uri.equals("/login") && !request.session.get("loginTime").isDefined) {
         //TODO: Make it so that after logging in successfully, the user gets redirected to the page that they originally wanted to go to
         //Note: changing the values in request.headers will not change session information
         var redirectedRequest = request.copy(request.id, request.tags, "/login", "/login", "GET", request.version, request.queryString, request.headers, request.remoteAddress)
         println("redirected request to [%s]" format redirectedRequest)
         super.onRouteRequest(redirectedRequest)
      } else {
         super.onRouteRequest(request)
      }
   }

   override def onRequestCompletion(request: RequestHeader) {
      println("completed request for [%s]" format request)
      super.onRequestCompletion(request)
   }

   override def onHandlerNotFound(request: RequestHeader) = {
      Future.successful(Results.NotFound(views.html.invalidPage()))
   }

}