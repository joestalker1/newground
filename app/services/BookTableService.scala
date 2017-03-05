package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import domain.{UpdateTable, _}
import domain.JsonConversion._
import cats.syntax.either._
import play.api.libs.json.{Reads, Writes}

class BookTableService(serviceId: String) extends PersistentActor with ActorLogging {
  override def persistenceId = serviceId

  var tables = List.empty[Table]
  var counter = 0l

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  def getId() = {
    val cur = counter
    counter += 1
    cur
  }

  def addTable(resp: AddTable): Unit = {
    val AddTable(afterId, table, _) = resp
    val ntable = table.copy(id = Some(getId))
    val newList = if (afterId == -1 || tables.isEmpty) ntable :: tables
    else {
      val nlist = tables.foldLeft(List.empty[Table]) { (l, t) =>
        if (t.id.exists(_ == afterId)) ntable :: t :: l
        else t :: l
      }
      nlist.reverse
    }
    tables = newList
    ntable
  }

  def updateTable(resp: UpdateTable): Unit = {
      val UpdateTable(table, _) = resp
      for {
        list <- Option(tables)
        tid <- table.id
        if (list.exists(_.id.exists(_ == tid)))
      } {
        val nlist = list.foldLeft(List.empty[Table]) { (res, t) =>
          if (t.id.exists(_ == tid)) table :: res
          else t :: res
        }
        tables = nlist.reverse
      }
      //table
  }

  def removeTable(resp: RemoveTable): Unit = {
    val RemoveTable(tid, _) = resp
    tables = tables.filterNot(_.id.exists(_ == tid))
  }

  def sendResponseAndBroadcast[A <: DomainObj : Writes](receiver: ActorRef, msg: A, broadMsg: Message): Unit = {
    receiver ! msg.json
    context.system.eventStream.publish(broadMsg)
  }

//  def receiveCommand1: Receive = {
//    case payload: String =>
//      println(s"persistentActor received ${payload} (nr = ${count})")
//      persist(payload + count) { evt =>
//        count += 1
//      }
//  }

  override def receiveRecover: Receive = {
    case _ =>
    //case _: String => count += 1
  }


  override def receiveCommand: Receive = LoggingReceive {
    case GetTables(receiver) => Option(tables).foreach(receiver ! TableList(_))

    case ServiceRequest(receiver, json) =>
      //try addTable
      val tryAddTable = json.domain[AddTable].map(persist(_)(addTable))
        //persist(cmd)(addTable)// req =>
          //val AddTable(afterId, _, _) = req
          //val list = tables
          //addTable(cmd)
//          if (tables.size > list.size) sendResponseAndBroadcast(receiver, TableAdded(afterId, ntable), TableAddedMsg)
          //}
      //}
      //try updateTable or removeTable
      tryAddTable.leftMap {_ =>
        json.domain[UpdateTable].map(persist(_)(updateTable))
             //val ntable = updateTable(cmd)
             //updateTable(cmd)
             //sendResponseAndBroadcast(receiver, TableUpdated(ntable), TableUpdatedMsg)
           //}
        } orElse {
          json.domain[RemoveTable].map(persist(_)(removeTable))
           //sendResponseAndBroadcast(receiver,TableRemoved(resp.id), TableRemovedMsg)
        }
      }
  //}

}

case class GetTables(receiver: ActorRef) extends Message

case object TableAddedMsg extends EmptyMessage

case object TableRemovedMsg extends EmptyMessage

case object TableUpdatedMsg extends EmptyMessage

