package ru.misis.routes

import scala.concurrent.ExecutionContext.Implicits.global


import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.service.Service
import ru.misis.model.Account.{CreateAccountRequest}
import ru.misis.model.ModelJsonFormats._

import scala.concurrent.Future

class Routes(service: Service)(implicit val system: ActorSystem) {
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val routes: Route =


    path("accounts") {
      get {
        complete(StatusCodes.OK, s"Account ${service.accountId}:\n${service.getSubAccounts}")
      }
    } ~
      path("amount" / IntNumber) { (index) =>
        get {
          complete(StatusCodes.OK, s"Account ${service.accountId} sub-account ${index} amount: ${service.getAmount(index)}")
        }
      } ~
      path("amount" / IntNumber / IntNumber / Segment) { (index, amount, category) =>
        post {
          onSuccess(service.update(index, amount, category)) {
            case Left(message) => complete(StatusCodes.BadRequest, message)
            case Right(value) => complete(StatusCodes.OK, "OK")
          }
        }
      } ~
      path("create") {
        post {
          entity(as[CreateAccountRequest]) { request =>
            onSuccess(service.create(request.amount)) {
              case Left(errorMessage) => complete(StatusCodes.BadRequest, errorMessage)
              case Right(_) => complete(StatusCodes.OK, "Account created")
            }
          }
        }
      } ~
      path("transfer" / IntNumber / IntNumber / IntNumber / Segment.?) { (sourceAccountId, targetAccountId, amount, category) =>
        post {

          onSuccess(service.transfer(sourceAccountId, targetAccountId, amount, category)) {
            case Left(errorMessage) => complete(StatusCodes.BadRequest, errorMessage)
            case Right(_) => complete(StatusCodes.OK, "Transfer successful")
          }

        }
      }
}



