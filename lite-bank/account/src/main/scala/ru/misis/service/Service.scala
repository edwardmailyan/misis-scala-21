package ru.misis.service

import akka.actor.ActorSystem
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}

class Service()(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {
}
