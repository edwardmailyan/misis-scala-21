package ru.misis.routes

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.model.Actor
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future

class ModelRoutes(bankRef: ActorRef[Actor.Command])(implicit val system: ActorSystem[_]){

    import ru.misis.model.ModelJsonFormats._

    val routes: Route =
    path("items") {
        get {
            onSuccess(Future.successful("OK")) { value =>
                complete(value.toJson.sortedPrint)
            }
        }
    }
}
