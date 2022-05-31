package ru.misis.model

import ru.misis.model.Account.AccountUpdated
import spray.json.DefaultJsonProtocol._

object ModelJsonFormats {
    implicit val formatAccountUpdated = jsonFormat4(AccountUpdated)
}
