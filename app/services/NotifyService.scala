package services

import java.util.concurrent.atomic.AtomicReference
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import domain.{SubscribeRequest, UnsubscribeRequest}
import domain.JsonConversion._

/**
  * Created by dfom on 22.01.2017.
  */
class NotifyService extends Actor with ActorLogging {
  val subscribers = new AtomicReference[List[ActorRef]](List.empty)

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  override def receive: Receive = LoggingReceive {
    case Request(wsOut, json) =>
      //get Subscribe
      val trySubcribe = json.domain[SubscribeRequest.type]
      trySubcribe.right.foreach{ _ =>
        Option(subscribers.get).orElse(Option(List.empty)).foreach(list  => subscribers.set(wsOut :: list))
        context.system.eventStream.publish(GetTables(wsOut))
      }
      //get Unsubcribe
      val tryUnsubcribe = trySubcribe.left.flatMap(_ => json.domain[UnsubscribeRequest.type])
      tryUnsubcribe.right.foreach(_ => Option(subscribers.get).map(_.filter(_ != wsOut)).foreach(subscribers.set(_)))
    case _ =>
  }

}
