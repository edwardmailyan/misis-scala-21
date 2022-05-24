package ru.misis.model

import ru.misis.event.Event

object Account {

    case class State(id: Int, amount: Int) {
        def update(value: Int) = copy(amount = amount + value)
    }

    case class AccountUpdated(accountId: Option[Int] = None, value: Int) extends Event

}