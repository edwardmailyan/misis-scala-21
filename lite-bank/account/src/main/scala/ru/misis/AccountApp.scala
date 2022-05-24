package ru.misis

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.routes.Routes
import ru.misis.service.Service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object AccountApp {

    private def startHttpServer(routes: Route, port: Int)(implicit system: ActorSystem): Unit = {

        val futureBinding = Http().newServerAt("localhost", port).bind(routes)
        futureBinding.onComplete {
            case Success(binding) =>
                val address = binding.localAddress
                system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
            case Failure(ex) =>
                system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
                system.terminate()
        }
    }

    val props = ElasticProperties("http://localhost:9200")
    val elastic = ElasticClient(JavaClient(props))

    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem("AccountApp")
        val config = system.settings.config
        val port = config.getInt("my-app.port")
        val accountId = config.getInt("my-app.accountId")

        val service = new Service(accountId)
        val routes = new Routes(service)
        startHttpServer(routes.routes, port)
    }
}
