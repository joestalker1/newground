package services

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain.{SubscribeRequest, TableList, UnsubscribeRequest}
import domain.JsonConversion._

/**
  * Sends notification table updates.
  *
  */
class NotifyService extends Actor with ActorLogging {
  val subscribers = new AtomicReference[List[ActorRef]](List.empty)

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  override def receive: Receive = LoggingReceive {
    case ServiceRequest(wsOut, json) =>
      //get Subscribe
      val trySubcribe = json.domain[SubscribeRequest.type]
      trySubcribe.right.foreach { _ =>
        Option(subscribers.get).foreach(list  => subscribers.set(wsOut :: list))
      }
      //get Unsubcribe
      for {
        _ <- trySubcribe.left
        _ <- json.domain[UnsubscribeRequest.type].right
      } yield Option(subscribers.get).map(_.filter(_ != wsOut)).foreach(subscribers.set(_))
    case TableAddedMsg | TableRemovedMsg | TableUpdatedMsg =>
      Option(subscribers.get).foreach(_ => context.system.eventStream.publish(GetTables(self)))
    case tlist : TableList => val json = tlist.json //receive from BookTableService
      Option(subscribers.get).foreach(list => list.foreach(_ ! json))
  }

}
