package ru.misis.model

import akka.actor.typed.Behavior

object Actor {

    sealed trait Command

    sealed trait Event

    def apply(): Behavior[Command] = ???
}
