package domain

sealed trait DomainObj

case class LoginRequest(username: String, password: String, $type: String = "login") extends DomainObj

case object LoginFailed extends DomainObj {
  val $type: String = "login_failed"
}

case class LoginSuccessful(userType: String, $type: String = "login_successful") extends DomainObj

case class Ping(seq: Int, $type: String = "ping") extends DomainObj

case class Pong(seq: Int, $type: String = "pong") extends DomainObj

case class Table(id: Int, name: String, participants: Int) extends DomainObj

case class TableList(tables: Seq[Table], $type: String = "table_list") extends DomainObj

case object SubscribeRequest extends DomainObj {
  val $type: String = "subscribe_tables"
}

case object UnsubscribeRequest extends DomainObj {
  val $type: String = "unsubscribe_tables"
}

case class TableAdded(afterId: Int, table: Table, $type: String = "table_added") extends DomainObj

case class TableUpdated(table: Table, $type: String = "table_updated") extends DomainObj

case class TableRemoved(id: Int, $type: String = "table_removed") extends DomainObj

