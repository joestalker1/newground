package controllers

import javax.inject.{Inject, Named}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.Timeout
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Controller, RequestHeader, WebSocket}
import org.reactivestreams.Publisher
import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import services.ApiGatewayLauncher
import akka.pattern._

/**
  * Web controller providers enty point for websocket clients.Creates [[ApiGateway]] to communicate with clients.
  *
  * @param apiGatewayLauncher
  * @param serviceLocator
  * @param actorSystem
  * @param mat
  * @param ec
  */
@Singleton
class AppController @Inject()(@Named("apiGatewayLauncher") apiGatewayLauncher: ActorRef, @Named("serviceLocator") serviceLocator: ActorRef)(implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext) extends Controller {

  private val logger = org.slf4j.LoggerFactory.getLogger("LoginController")

  def ws: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    toFutureFlow(rh).map { flow =>
      Right(flow)
    }.recover {
      case e: Exception =>
        logger.error("Cannot create websocket", e)
        val jsError = Json.obj("error" -> "Cannot create websocket to login")
        val result = InternalServerError(jsError)
        Left(result)
    }

  }

  def toFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    val (webSocketOut: ActorRef, webSocketIn: Publisher[JsValue]) = createWebSocketConnections()
    val serviceFuture = createApiGateway(webSocketOut)
    serviceFuture.map { service =>
      createWebSocketFlow(webSocketIn, service)
    }
  }

  def createWebSocketConnections(): (ActorRef, Publisher[JsValue]) = {
    val source: Source[JsValue, ActorRef] = {
      val logging = Logging(actorSystem.eventStream, logger.getName)
      //if inner buffer is overflowed it will remove more nw messages.
      Source.actorRef[JsValue](10, OverflowStrategy.dropTail).log("actorRefSource")(logging)
    }

    val sink: Sink[JsValue, Publisher[JsValue]] = Sink.asPublisher(fanout = false)

    source.toMat(sink)(Keep.both).run()
  }

  def createWebSocketFlow(webSocketIn: Publisher[JsValue], service: ActorRef): Flow[JsValue, JsValue, NotUsed] = {
    val flow = {
      val sink = Sink.actorRef(service, akka.actor.Status.Success(()))
      val source = Source.fromPublisher(webSocketIn)
      Flow.fromSinkAndSource(sink, source)
    }

    val flowWatch: Flow[JsValue, JsValue, NotUsed] = flow.watchTermination() { (_, termination) =>
      termination.foreach { done =>
        logger.info(s"Terminating actor $service")
        actorSystem.stop(service)
      }
      NotUsed
    }
    flowWatch
  }

  def createApiGateway(webSocketOut: ActorRef): Future[ActorRef] = {
    // Use guice assisted injection
    implicit val timeout = Timeout(100.millis)
    (apiGatewayLauncher ? ApiGatewayLauncher.Create(webSocketOut)).mapTo[ActorRef]
  }
}