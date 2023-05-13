package ru.misis.model

import ru.misis.event.Event
import scala.collection.mutable.Map

object Account {

    case class State(id: Int,  accounts: Map[Int, Int]) {
        def update(accountId: Int, value: Int): State = {
            val updatedAmount = accounts.getOrElse(accountId, 0) + value
            val updatedAccounts = accounts.updated(accountId, updatedAmount)
            copy(accounts = updatedAccounts)
        }
    }

    case class AccountUpdated(accountId: Option[Int] = None,
                              index: Option[Int] = None,
                              value: Int,
                              category: Option[String] = None,
                              needCommit: Option[Boolean] = Some(false)) extends Event
    case class CreateAccountRequest(amount: Int)

    case class TransferRequest(sourceSubAccount: Int,
                               targetAccountId: Int,
                               targetSubAccount: Int,
                               amount: Int) extends Event

}