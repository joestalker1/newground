package services

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain.{UpdateTable, _}
import domain.JsonConversion._


/**
  * Created by dfom on 22.01.2017.
  */

class BookTableService extends Actor with ActorLogging {
  val tables = new AtomicReference[List[Table]](List.empty[Table])
  val counter = new AtomicLong(0)

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  def getId() = counter.getAndIncrement()

  override def receive: Receive = LoggingReceive {
    case GetTables(receiver) => Option(tables.get).foreach(receiver ! TableList(_))

    case Request(receiver, json)=>
       val tryAddTable =  json.domain[AddTable]
       tryAddTable.right.foreach{case AddTable(afterId, table, _) =>
           Option(tables.get()).map{list =>
             val ntable = table.copy(id = Some(getId))
             val newList = if(afterId == -1 || list.isEmpty) ntable :: list
             else {
               val nlist = list.foldLeft(List.empty[Table]){(l,t) =>
                 if(t.id.exists(_ == afterId))  ntable :: t :: l
                 else t :: l
               }
               nlist.reverse
             }
             tables.set(newList)
             if(newList.size > list.size) {
               receiver ! TableAdded(afterId, ntable).json
               context.system.eventStream.publish(TableAddedMsg)
             }
           }
       }
      for {
         _ <- tryAddTable.left
         r  <- json.domain[UpdateTable].right
      } yield {
        val UpdateTable(table, _) = r
        for {
          list <- Option(tables.get)//Option and Either are different monads I can't combine them.
          tid <- table.id
          if(list.exists(_.id.exists(_ == tid)))
        } yield {
          val nlist = list.foldLeft(List.empty[Table]){(res, t) =>
            if(t.id.exists(_ == tid))  table :: res
            else t :: res
          }
          tables.set(nlist.reverse)
          receiver ! TableUpdated(table).json
          context.system.eventStream.publish(TableUpdatedMsg)
        }
       }
      for {
        _ <- tryAddTable.left
        _ <- json.domain[UpdateTable].left
        r <- json.domain[RemoveTable].right
      } yield {
        val RemoveTable(tid, _) = r
        Option(tables.get).filter(_.exists(_.id.exists(_ == tid))).foreach{lst =>
          tables.set(lst.filterNot(_.id.exists(_ == tid)))
          receiver ! TableRemoved(tid).json
          context.system.eventStream.publish(TableRemovedMsg)
        }
      }

  }

}

trait EmptyMessage extends Message {
  val receiver: ActorRef = ActorRef.noSender
}
case class GetTables(receiver: ActorRef) extends Message
case object TableAddedMsg extends EmptyMessage
case object TableRemovedMsg extends EmptyMessage
case object TableUpdatedMsg extends EmptyMessage

