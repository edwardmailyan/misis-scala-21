package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import ru.misis.model.Account.{AccountUpdated, State, TransferRequest}
import ru.misis.util.WithKafka
import scala.util.{Success, Failure}
import scala.collection.mutable

import scala.collection.mutable.Map

import scala.concurrent.{ExecutionContext, Future}

class Service(val accountId: Int)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

  import ru.misis.model.ModelJsonFormats._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private var state: State = State(accountId, Map.empty)

  def getSubAccounts: String = state.accounts.mkString("\n")

  def getAmount(index: Int): Int = state.accounts(index)

  def update(index: Int, value: Int, category: String) = {
    if (state.accounts(index) + value < 0)
      Future.successful(Left("Недостаточно средств на счете"))
    else {
      publishEvent(AccountUpdated(Some(accountId), Some(index), value, Some(category))).map(Right(_))
    }
  }

  def transfer(sourceSubAccount: Int,
               targetAccountId: Int,
               targetSubAccount: Int,
               amount: Int): Future[Either[String, Unit]] = {
    if (targetAccountId == accountId) {
      if (targetSubAccount == sourceSubAccount){
        Future.successful(Left("Cannot transfer funds to the same account"))
      } else {
        publishEvent(AccountUpdated(Some(state.id), Some(targetSubAccount), -amount, Some("category"))).map(Right(_))
      }
    } else if (amount <= 0) {
      Future.successful(Left("Invalid transfer amount"))
    } else if (state.accounts(sourceSubAccount) < amount) {
      Future.successful(Left("Invalid transfer amount"))
    } else {
      publishEvent(AccountUpdated(Some(state.id), Some(sourceSubAccount), -amount, Some("category"))).map(Right(_))
      publishEvent(AccountUpdated(Some(targetAccountId), Some(targetSubAccount), amount, Some("category"))).map(Right(_))
    }
  }


  /*
      send -amount
      commit (save offset)
      send +amount
   */
  def snapshot(index: Int): Future[Unit] = {
    val amount = getAmount(index)
    Source(Seq(
      AccountUpdated(Some(accountId), Some(index), - amount, None, Some(true)),
      AccountUpdated(Some(accountId), Some(index), + amount)
    )).runWith(kafkaSink).map(_ => ())
  }

  /*
  Snapshot:
   - сохранить состояние
   - сохранить offset
   */
  kafkaCSource[AccountUpdated]
    .filter {
      case (_, AccountUpdated(Some(id), _, _,  _, _)) =>
        id == accountId
      case (_, event) =>
        logger.info(s"Empty account ${event}")
        false
    }
    .map { case message @ (_, AccountUpdated(_, Some(index), value,  _, _)) =>
      state = state.update(index, value)
      logger.info(s"State updated ${value} ${state}")
      message
    }
    .filter { case (_, AccountUpdated(_, _, _, _, needCommit)) => needCommit.getOrElse(false)}
    .map { case (offset, _) => offset }
    .log("AccountUpdated error")
    .runWith(committerSink)


  def create(amount: Int): Future[Either[String, Unit]] = {
    if (amount < 0)
      Future.successful(Left("Invalid amount"))
    else {

      val newAccountId = if (state.accounts.isEmpty) 0 else state.accounts.keys.max + 1
      state.accounts += (newAccountId -> amount)

      snapshot(newAccountId).map(_ => {
        logger.info(s"Account $accountId created with initial balance $amount")
        Right(())
      }).recover {
        case ex =>
          logger.error(s"Error creating account $accountId: $ex")
          Left(s"Error creating account $accountId: ${ex.getMessage}")
      }
    }
  }


}


