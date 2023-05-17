package ru.misis.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.service.Service
import spray.json.DefaultJsonProtocol._

class Routes(service: Service)(implicit val system: ActorSystem) {

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val routes: Route =
    path("categories") {
      get {
        complete(StatusCodes.OK, s"Categories:\n${service.getCategories}")
      }
    } ~
      path("create" / Segment / IntNumber) { (category, percent) =>
        post {
          service.addCategory(category, percent)
          complete(StatusCodes.OK, s"Category ${category} created with ${percent}% cashback")
        }
      } ~
      path("report") {
        get {
          complete(StatusCodes.OK, service.categories.toMap)
        }
      }
}
