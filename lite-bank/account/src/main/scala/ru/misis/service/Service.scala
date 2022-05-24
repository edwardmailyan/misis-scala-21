package ru.misis.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import ru.misis.model.Account.{AccountUpdated, State}
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}

class Service(val accountId: Int)(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {

    import ru.misis.model.ModelJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    private var state: State = State(accountId, 0)

    def getAmount: Int = state.amount

    def update(value: Int) = {
        if (state.amount + value < 0)
            Future.successful(Left("Недостаточно средств на счете"))
        else {
            publishEvent(AccountUpdated(Some(accountId), value)).map(Right(_))
        }
    }

    kafkaSource[AccountUpdated]
        .filter {
            case AccountUpdated(Some(id), _) =>
                id == accountId
            case event =>
                logger.info(s"Empty account ${event}")
                false
        }
        .map { case AccountUpdated(_, value) =>
            state = state.update(value)
            logger.info(s"State updated ${value} ${state}")
        }
        .runWith(Sink.ignore)
}
