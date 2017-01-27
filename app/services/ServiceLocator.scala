package services

import javax.inject._

import akka.actor.{Actor, ActorSystem, Props}
import play.api.Configuration

/**
  * Locate services using by [[ApiGateway]].It creates and locates services for [[ApiGateway]].
  */
class ServiceLocator @Inject()(conf: Configuration) extends Actor {
  val userLoginService = context.system.actorOf(Props(new UserLoginService(conf)), "userLoginService:" + System.nanoTime())
  val pingService = context.system.actorOf(Props[PingService], "pingService:" + System.nanoTime())
  val notifyService = context.system.actorOf(Props[NotifyService], "notifyService:" + System.nanoTime())
  val bookTableService = context.system.actorOf(Props[BookTableService], "bookTableService" + System.nanoTime())

  override def receive: Receive = {
    case GetUserLoginService => sender ! userLoginService
    case GetPingService => sender ! pingService
    case GetNotifyService => sender ! notifyService
    case GetBookTableService => sender ! bookTableService
  }
}

case object GetUserLoginService extends EmptyMessage

case object GetPingService extends EmptyMessage

case object GetNotifyService extends EmptyMessage

case object GetBookTableService extends EmptyMessage