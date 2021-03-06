package services

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.event.LoggingReceive
import com.google.inject.assistedinject.Assisted
import play.api.libs.json.JsValue
import javax.inject._

import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._
import akka.pattern._
import akka.util.Timeout
import domain.{LoginRequest, LoginSuccessful, NotAuthorized}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import domain.JsonConversion._
import cats._
import cats.implicits._
import cats.syntax.either._

/**
  * Communicate with a clients, govern user authorization to services.Holds an actor to work with a websocket client.For every clients it creates own [[ApiGateway]].
  * But all that share services.
  *
  */
class ApiGateway @Inject()(@Assisted wsOut: ActorRef, @Named("serviceLocator") serviceLocator: ActorRef) extends Actor with ActorLogging {
  val nameToMessage = Map("login" -> GetUserLoginService,
    "subscribe_tables" -> GetNotifyService,
    "unsubscribe_tables" -> GetNotifyService,
    "ping" -> GetPingService,
    "remove_table" -> GetBookTableService,
    "update_table" -> GetBookTableService,
    "add_table" -> GetBookTableService)

  val prohibitedUserOps = Set("add_table", "update_table", "remove_table")

  override def receive: Receive = LoggingReceive {
    case json: JsValue =>
      json.domain[LoginRequest].map(_ => findAndCall("login", json, self))
        .leftMap{_ =>
          val tryLogingSuc = json.domain[LoginSuccessful]
          tryLogingSuc.map { resp =>
             selectHandler(resp)
             wsOut forward json
          }
        }
    case _ =>
  }

  def selectHandler(resp: LoginSuccessful): Unit = if (isAdmin(resp.userType)) context.become(forAdmin) else context.become(forUser)

  def forAdmin: Receive = LoggingReceive {
    case json: JsValue =>
      implicit val timeout = Timeout(100.millis)
      (json \ "$type").toOption match {
        case Some(service: JsString) => findAndCall(service.value, json, wsOut)
        case _ =>
      }
    case Terminated(actor) => context.unwatch(actor)
  }

  def forUser: Receive = LoggingReceive {
    case json: JsValue =>
      implicit val timeout = Timeout(100.millis)
      (json \ "$type").toOption match {
        case Some(s: JsString) => if (prohibitedUserOps(s.value)) wsOut ! NotAuthorized.json
        else findAndCall(s.value, json, wsOut) ///Added filter by table requests!!!
        case _ =>
      }
    case _ =>
  }

  private def isAdmin(user: String): Boolean = user == "admin"

  private def findAndCall(serviceName: String, json: JsValue, receiver: ActorRef): Unit = {
    implicit val timeout = Timeout(100.millis)
    nameToMessage.get(serviceName).foreach { msg =>
      (serviceLocator ? msg).mapTo[ActorRef].map(_ ! ServiceRequest(receiver, json))
    }
  }
}

trait ApiGatewayFactory {
  def apply(wsOut: ActorRef): Actor
}

case class ServiceRequest(receiver: ActorRef, payload: JsValue) extends Message

class ApiGatewayLauncher @Inject()(factory: ApiGatewayFactory) extends Actor with InjectedActorSupport with ActorLogging {

  import ApiGatewayLauncher._

  override def receive: Receive = LoggingReceive {
    case Create(clientOut) =>
      val child: ActorRef = injectedChild(factory(clientOut), "apiGateway:" + System.nanoTime())
      sender() ! child
  }
}


object ApiGatewayLauncher {

  case class Create(clientOut: ActorRef)

}