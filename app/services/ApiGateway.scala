package services

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import com.google.inject.assistedinject.Assisted
import play.api.libs.json.JsValue
import javax.inject._
import play.api.libs.concurrent.InjectedActorSupport

/**
  * Created by dfom on 22.01.2017.
  */
class ApiGateway @Inject()(@Assisted wsOut: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = LoggingReceive {
    case json: JsValue => context.system.eventStream.publish(Request(wsOut, json))
  }
}

trait ApiGatewayFactory {
  def apply(wsOut: ActorRef): Actor
}

case class Request(receiver: ActorRef, payload: JsValue) extends Message

class ApiGatewayLauncher @Inject()(factory: ApiGatewayFactory) extends Actor with InjectedActorSupport with ActorLogging {

  import ApiGatewayLauncher._

  override def receive: Receive = LoggingReceive {
    case Create(out) =>
      val child: ActorRef = injectedChild(factory(out), "apiGateway:" + System.nanoTime())
      sender() ! child
  }
}


object ApiGatewayLauncher {
  case class Create(out: ActorRef)
}