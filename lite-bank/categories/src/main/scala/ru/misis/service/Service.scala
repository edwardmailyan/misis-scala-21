package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.StatusCodes
import ru.misis.model.Account.AccountUpdated
import ru.misis.util.WithKafka

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class Service()(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {
  private val logger = LoggerFactory.getLogger(this.getClass)

  import ru.misis.model.ModelJsonFormats._

  val categories = mutable.Map[String, Int]()

  def getCategories: String = categories.mkString("\n")

  def addCategory(name: String, percent: Int): mutable.Map[String, Int] = {
    categories += (name -> percent)
    categories
  }

  def calculateCashback(category: String, value: Int): Int = {
    categories.get(category) match {
      case Some(percent) =>
        (value * percent) / 100
      case None =>
        0
    }
  }

  kafkaSource[AccountUpdated]
    .filter(event => event.category.isDefined)
    .mapAsync(1) { event =>
      val categoryOpt = event.category.get
      val cashback = calculateCashback(categoryOpt, event.value)
      val cashbackEvent = AccountUpdated(event.accountId, -cashback)
      publishEvent(cashbackEvent)
    }
    .runWith(Sink.ignore)
}
