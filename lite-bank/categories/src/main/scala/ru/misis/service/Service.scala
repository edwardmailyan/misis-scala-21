package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import ru.misis.model.Account.AccountUpdated
import ru.misis.util.WithKafka

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class Service()(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {
    private val logger = LoggerFactory.getLogger(this.getClass)
    import ru.misis.model.ModelJsonFormats._

    val state = mutable.Map[String, Int]()

    kafkaSource[AccountUpdated]
        .filter(event => event.category.isDefined)
        .map { event =>
            val category = event.category.get
            val amount = state.getOrElse(category, 0)
            state.put(category, event.value + amount)
            logger.info(s"State updated ${category} ${state(category)}")
        }
        .runWith(Sink.ignore)

}

