package services

import java.util.concurrent.atomic.{AtomicReference}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain.{Table, TableList}
import domain.JsonConversion._


/**
  * Created by dfom on 22.01.2017.
  */

class BookTableService extends Actor with ActorLogging {
  val tables = new AtomicReference[List[Table]](List.empty[Table])

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  override def receive: Receive = LoggingReceive {
    case GetTables(receiver) => Option(tables.get).foreach(receiver ! TableList(_).json)
  }

}

case class GetTables(receiver:ActorRef) extends Message