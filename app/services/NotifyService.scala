package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain.{SubscribeRequest, TableList, UnsubscribeRequest}
import domain.JsonConversion._
import cats.syntax.either._

/**
  * Sends notification table updates.
  *
  */
class NotifyService extends Actor with ActorLogging {
  var subscribers = List.empty[ActorRef]

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  override def receive: Receive = LoggingReceive {
    case ServiceRequest(wsOut, json) =>
      //get Subscribe or Unsubcribe
      val trySubcribe = json.domain[SubscribeRequest.type].map(_ => Option(subscribers).foreach(list  => subscribers = wsOut :: list))

      trySubcribe.orElse(json.domain[UnsubscribeRequest.type]
          .map(_ => Option(subscribers).map(_.filter(_ != wsOut)).foreach(subscribers = _)))

    case TableAddedMsg | TableRemovedMsg | TableUpdatedMsg =>
      Option(subscribers).foreach(_ => context.system.eventStream.publish(GetTables(self)))

    case tlist : TableList => val json = tlist.json //receive from BookTableService
      Option(subscribers).foreach(list => list.foreach(_ ! json))
  }

}
