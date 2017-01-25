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
    case GetTables(receiver) => Option(tables.get).foreach(receiver ! TableList(_).json)

    case Request(receiver, json)=>
       val tryAddTable =  json.domain[AddTable]
       tryAddTable.right.foreach{case AddTable(afterId, table, _) =>
           Option(tables.get()).map{list =>
             val newList = if(afterId == -1 || list.isEmpty) table.copy(id = Some(getId)) :: list
             else {
               val left = list.takeWhile(_.id.exists(_!= afterId))
               val diff = list.diff(left)
               if(diff.nonEmpty) {
                 val right = diff.tail
                 val after = diff.head
                 (left :+ after :+ table.copy(id = Some(getId))) ++ right
               } else left :+ table.copy(id=Some(getId))
             }
             tables.set(newList)
             receiver ! TableAdded(afterId, table).json
           }
       }
      for {
         _ <- tryAddTable.left
         r  <- json.domain[UpdateTable].right
      } yield {
        val UpdateTable(table, _) = r
        Option(tables.get).filter(_.exists(_.id == table.id)).foreach{list =>
          val left = list.takeWhile(_.id != table.id)//last is table that is changed
          val nleft = if(left.nonEmpty) left.init else left
          val right = list.diff(left)
          val nright = if(right.nonEmpty) right.tail else right
          tables.set((nleft :+ table) ++ nright)
          receiver ! TableUpdated(table).json
        }
      }
      for {
        _ <- tryAddTable.left
        _ <- json.domain[UpdateTable].left
        r <- json.domain[RemoveTable].right
      } yield {
        val RemoveTable(id,_) = r
        Option(tables.get).foreach{list =>
          val newList = list.filter(_.id != id)
          tables.set(newList)
          receiver ! TableRemoved(id).json
        }
      }
  }

}

case class GetTables(receiver:ActorRef) extends Message