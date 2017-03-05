package domain

/**
  * Domain objects
  */
sealed trait DomainObj

case class LoginRequest(username: String, password: String, $type: String = "login") extends DomainObj

case object LoginFailed extends DomainObj {
  val $type: String = "login_failed"
}

case class LoginSuccessful(userType: String, $type: String = "login_successful") extends DomainObj

case class Ping(seq: Int, $type: String = "ping") extends DomainObj

case class Pong(seq: Int, $type: String = "pong") extends DomainObj

case class Table(name: String, participants: Int, id: Option[Long] = None) extends DomainObj

case class TableList(tables: Seq[Table], $type: String = "table_list") extends DomainObj

case object SubscribeRequest extends DomainObj {
  val $type: String = "subscribe_tables"
}

case object UnsubscribeRequest extends DomainObj {
  val $type: String = "unsubscribe_tables"
}


case class AddTable(after_id: Int, table: Table, $type: String = "add_table") extends DomainObj
case class TableAdded(after_id: Int, table: Table, $type: String = "table_added") extends DomainObj

case class UpdateTable(table: Table, $type: String = "update_table") extends DomainObj
case class TableUpdated(table: Table, $type: String = "table_updated") extends DomainObj

case class RemoveTable(id: Int, $type: String = "remove_table") extends DomainObj
case class TableRemoved(id: Int, $type: String = "table_removed") extends DomainObj

case class RemovalFailed(id: Int, $type: String = "removal_failed") extends DomainObj

case class UpdateFailed(id: Int, $type: String = "update_failed") extends DomainObj

case object NotAuthorized extends DomainObj {
  val $type = "not_authorized"
}