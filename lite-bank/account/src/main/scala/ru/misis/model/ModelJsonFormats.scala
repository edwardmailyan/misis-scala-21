package ru.misis.model

import ru.misis.model.Account.{AccountUpdated, CreateAccountRequest, TransferRequest}
import spray.json.DefaultJsonProtocol._

object ModelJsonFormats {
    implicit val formatAccountUpdated = jsonFormat4(AccountUpdated)

    implicit val formatCreateAccountRequest = jsonFormat1(CreateAccountRequest)
    implicit val formatTransferRequest = jsonFormat3(TransferRequest)
}

