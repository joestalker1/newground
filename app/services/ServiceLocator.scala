package services

import javax.inject._

import akka.actor.{Actor, ActorLogging, Props, Terminated}
import akka.event.LoggingReceive
import play.api.Configuration

/**
  * Locate services using by [[ApiGateway]].It creates and locates services for [[ApiGateway]].
  */
class ServiceLocator @Inject()(conf: Configuration) extends Actor with ActorLogging {
  val userLoginService = context.system.actorOf(Props(new UserLoginService(conf)), "userLoginService:" + System.nanoTime())
  val pingService = context.system.actorOf(Props[PingService], "pingService:" + System.nanoTime())
  val notifyService = context.system.actorOf(Props(new NotifyService), "notifyService:" + System.nanoTime())
  val bookTableService = context.system.actorOf(Props(new BookTableService), "bookTableService" + System.nanoTime())

  context.watch(userLoginService)
  context.watch(pingService)
  context.watch(notifyService)
  context.watch(bookTableService)

  override def receive: Receive = LoggingReceive {
    case GetUserLoginService => sender ! userLoginService
    case GetPingService => sender ! pingService
    case GetNotifyService => sender ! notifyService
    case GetBookTableService => sender ! bookTableService
    case Terminated(actor) => context.unwatch(actor)
  }
}

case object GetUserLoginService extends EmptyMessage

case object GetPingService extends EmptyMessage

case object GetNotifyService extends EmptyMessage

case object GetBookTableService extends EmptyMessage