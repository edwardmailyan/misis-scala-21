package ru.misis

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.model.Actor
import ru.misis.routes.ModelRoutes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object SampleApp {
    private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {

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
        val rootBehavior = Behaviors.setup[Nothing] { context =>

            implicit val contextImpl = context.system
            implicit val executionContext = contextImpl.executionContext


            val bankActor = context.spawn(Actor(), "Actor")
            context.watch(bankActor)
            val routes = new ModelRoutes(bankActor)
            startHttpServer(routes.routes)

            Behaviors.empty
        }
        val system = ActorSystem[Nothing](rootBehavior, "LiteBank")
    }
}
