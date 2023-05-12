package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import ru.misis.model.Account.{AccountUpdated, State, TransferRequest}
import ru.misis.util.WithKafka
import scala.util.{Success, Failure}


import scala.concurrent.{ExecutionContext, Future}

class Service(val accountId: Int)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

  import ru.misis.model.ModelJsonFormats._

  private val logger = LoggerFactory.getLogger(this.getClass)

  private var state: State = State(accountId, 0)

  def getAmount: Int = state.amount

  def update(value: Int, category: String) = {
    if (state.amount + value < 0)
      Future.successful(Left("Недостаточно средств на счете"))
    else {
      publishEvent(AccountUpdated(Some(accountId), value, Some(category))).map(Right(_))
    }
  }

  def transfer(targetAccountId: Int, amount: Int): Future[Either[String, Unit]] = {
    if (targetAccountId == accountId) {
      Future.successful(Left("Cannot transfer funds to the same account"))
    } else if (amount <= 0) {
      Future.successful(Left("Invalid transfer amount"))
    } else {
      val transferRequest = TransferRequest(targetAccountId, amount)
      update(-amount, "Transfer to " + targetAccountId).flatMap {
        case Left(errorMessage) => Future.successful(Left(errorMessage))
        case Right(_) => update(amount, "Transfer from " + accountId).flatMap {
          case Left(errorMessage) => Future.successful(Left(errorMessage))
          case Right(_) => publishEvent(transferRequest).map(_ => Right(()): Either[String, Unit])
        }
      }
    }
  }


  /*
      send -amount
      commit (save offset)
      send +amount
   */
  def snapshot(): Future[Unit] = {
    val amount = getAmount
    Source(Seq(
      AccountUpdated(Some(accountId), - amount, None, Some(true)),
      AccountUpdated(Some(accountId), + amount)
    )).runWith(kafkaSink).map(_ => ())
  }

  /*
  Snapshot:
   - сохранить состояние
   - сохранить offset
   */
  kafkaCSource[AccountUpdated]
    .filter {
      case (_, AccountUpdated(Some(id), _,  _, _)) =>
        id == accountId
      case (_, event) =>
        logger.info(s"Empty account ${event}")
        false
    }
    .map { case message @ (_, AccountUpdated(_, value,  _, _)) =>
      state = state.update(value)
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
      state = State(accountId, amount)
      snapshot().map(_ => {
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


