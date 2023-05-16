package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import ru.misis.model.Account.{AccountUpdated, State, TransferRequest}
import ru.misis.util.WithKafka
import scala.collection.mutable
import scala.util.{Success, Failure}

import scala.collection.mutable.Map

import scala.concurrent.{ExecutionContext, Future}

class Service(val accountId: Int)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

  import ru.misis.model.ModelJsonFormats._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private var state: State = State(mutable.Map(accountId -> 0))

  def getSubAccounts: String = state.accounts.mkString("\n")

  def getAmount(index: Int): Int = state.accounts(index)

  def update(index: Int, value: Int, category: String) = {
    if (state.accounts(index) + value < 0)
      Future.successful(Left("Недостаточно средств на счете"))
    else {
      publishEvent(AccountUpdated(Some(accountId), value, Some(category))).map(Right(_))
    }
  }

  def transfer(sourceAccountId: Int,
               targetAccountId: Int,
               amount: Int): Future[Either[String, Unit]] = {
    if (amount <= 0) {
      Future.successful(Left("Invalid transfer amount"))
    } else if (state.accounts(sourceAccountId) + amount < 0) {
      Future.successful(Left("Invalid transfer amount"))
    } else {
      publishEvent(AccountUpdated(Some(sourceAccountId), -amount, Some("category"))).map(Right(_))
      publishEvent(AccountUpdated(Some(targetAccountId), amount, Some("category"))).map(Right(_))
    }
  }


  kafkaCSource[AccountUpdated]
    .filter {
      case (_, AccountUpdated(Some(id), _,  _, _)) =>
        state.accounts.contains(id)
      case (_, event) =>
        logger.info(s"Empty account ${event}")
        false
    }
    .map { case message @ (_, AccountUpdated(Some(id), value, category, _)) =>
      state = state.update(id, value)
      logger.info(s"State updated ${value} ${state}")
      message
    }
    .filter { case (_, AccountUpdated(_, _, _, needCommit)) => needCommit.getOrElse(false)}
    .map { case (offset, _) => offset }
    .log("AccountUpdated error")
    .runWith(committerSink)




  def create(amount: Int): Future[Either[String, Unit]] = {
    if (amount < 0)
      Future.successful(Left("Invalid amount"))
    else {

      val newAccountId = if (state.accounts.isEmpty) 0 else state.accounts.keys.max + 1
      state.accounts += (newAccountId -> amount)
      Future.successful(Right())

    }
  }


}


