package services

import javax.inject._

import akka.actor.{Actor, ActorSystem, Props, Terminated}
import akka.persistence.journal.leveldb.SharedLeveldbStore
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

/**
  * Locate services using by [[ApiGateway]].It creates and locates services for [[ApiGateway]].
  */
class ServiceLocator @Inject()(conf: Configuration) extends Actor {
  val logger = LoggerFactory.getLogger("ServiceLocator")
  val userLoginService = context.system.actorOf(Props(new UserLoginService(conf)), "userLoginService:" + System.nanoTime())
  val pingService = context.system.actorOf(Props[PingService], "pingService:" + System.nanoTime())
  val persistenceId = "bookTableService1"
  val storeDB = context.system.actorOf(Props[SharedLeveldbStore], "SharedStore")
  val bookTableService = context.system.actorOf(Props(new BookTableService(persistenceId, storeDB)), "bookTableService" + System.nanoTime())
  val notifyService = context.system.actorOf(Props(new NotifyService(persistenceId)), "notifyService:" + System.nanoTime())

  override def preStart():Unit = {
    context.watch(bookTableService)
    context.watch(userLoginService)
    context.watch(pingService)
    context.watch(notifyService)
  }

  override def receive: Receive = {
    case GetUserLoginService => sender ! userLoginService
    case GetPingService => sender ! pingService
    case GetNotifyService => sender ! notifyService
    case GetBookTableService => sender ! bookTableService
    case Terminated(actor) => logger.error(s"$actor is terminated.")
        context.unwatch(actor)
  }
}

case object GetUserLoginService extends EmptyMessage

case object GetPingService extends EmptyMessage

case object GetNotifyService extends EmptyMessage

case object GetBookTableService extends EmptyMessage