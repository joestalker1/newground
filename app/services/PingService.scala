package services

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import domain.{Ping, Pong}
import domain.JsonConversion._

/**
  * Created by dfom on 22.01.2017.
  */
class PingService extends Actor with ActorLogging {
  val seq = new AtomicLong()

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  override def receive: Receive = LoggingReceive {
    case ServiceRequest(wsOut, json) =>
      json.domain[Ping] match {
        case Right(Ping(seq, _)) => wsOut ! Pong(seq).json
        case Left(ex: Throwable) =>
      }
    case _ =>
  }

}
