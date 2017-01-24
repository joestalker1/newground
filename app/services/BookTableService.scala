package services

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain._
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

    case Request(receiver,json)=>
       val tryAddTable =  json.domain[AddTable]
       tryAddTable.right.foreach{case AddTable(afterId, table, _) =>
           Option(tables.get()).map{list =>
             val newList = if(afterId == -1 || list.isEmpty) list :+ table
             else {
               val left = list.takeWhile(_.id != afterId)
               val diff = list.diff(left)
               val right = diff.tail
               val after = diff.head
               (left :+ after :+ table) ++ right
             }
             tables.set(newList)
           }
       }
       val tryUpdateTable = tryAddTable.left.flatMap(_ => json.domain[UpdateTable])
       tryUpdateTable.right.foreach{case UpdateTable(table,_) =>
           Option(tables.get).filter(_.exists(_.id == table.id)).foreach{list =>
             val left = list.takeWhile(_.id != table.id)
             val right = list.diff(left).tail
             tables.set((left :+ table) ++ right)
           }
       }
       val tryRemoveTable = tryUpdateTable.left.flatMap( _ => json.domain[RemoveTable])
       tryRemoveTable.right.foreach{case RemoveTable(id,_) =>
           Option(tables.get).foreach{list =>
              val newList = list.filter(_.id != id)
              tables.set(newList)
           }
       }
  }

}

case class GetTables(receiver:ActorRef) extends Message