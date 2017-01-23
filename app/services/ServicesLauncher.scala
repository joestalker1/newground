package services

import javax.inject._

import akka.actor.{ActorSystem, Props}
import play.api.Configuration

/**
  * Created by dfom on 23.01.2017.
  */
@Singleton
class ServicesLauncher @Inject()(actorSystem: ActorSystem, conf: Configuration) {
  //launch all needed services:
  val userLoginService = actorSystem.actorOf(Props(new UserLoginService(conf)), "userLoginService:" + System.nanoTime())
  val pingService = actorSystem.actorOf(Props[PingService], "pingService:" + System.nanoTime())
  val notifyService = actorSystem.actorOf(Props[NotifyService], "notifyService:" + System.nanoTime())
  val bookTableService = actorSystem.actorOf(Props[BookTableService], "bookTableService" + System.nanoTime())

}
