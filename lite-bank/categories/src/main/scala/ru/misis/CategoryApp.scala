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

object CategoryApp {
    private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {

        val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
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
        implicit val system = ActorSystem("CategoryApp")
        val service = new Service()
        val routes = new Routes(service)
        startHttpServer(routes.routes)
    }
}
