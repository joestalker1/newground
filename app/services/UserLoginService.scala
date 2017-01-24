package services

import akka.actor._
import akka.event.LoggingReceive
import domain.{LoginFailed, LoginRequest, LoginSuccessful}
import play.api.Configuration
import domain.JsonConversion._

class UserLoginService (conf: Configuration) extends Actor with ActorLogging {
  val credentials = loadCredentials()

  override def preStart = context.system.eventStream.subscribe(self, classOf[Message])

  override def postStop = context.system.eventStream.unsubscribe(self)

  def loadCredentials(): Map[String, String] = {
    import collection.JavaConversions._
    val users = conf.getConfig("users").get
    val strings = for ((user, pass) <- users.entrySet) yield (user, pass.unwrapped().toString)
    strings.toMap
  }

  override def receive: Receive = LoggingReceive {
    case Request(wsOut, json) =>
      json.domain[LoginRequest] match {
        case Right(LoginRequest(user, password, _)) => checkCredentialAndSendAnswer(wsOut, user, password)
        case Left(ex: Throwable) =>
      }
  }

  private def checkCredentialAndSendAnswer(wsOut:ActorRef, user: String, password: String): Unit = {
    val expected = credentials.get(user)
    val response = expected.filter(_ == password).map(_ => LoginSuccessful(user).json).orElse(Some(LoginFailed.json))
    response.foreach(resp => wsOut ! resp)

  }
}

trait UserLoginServiceFactory {
  def apply(wsOut: ActorRef): Actor
}
