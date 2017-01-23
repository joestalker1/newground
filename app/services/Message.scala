package services

import akka.actor.ActorRef

/**
  * Created by dfom on 23.01.2017.
  */
trait Message {
   val receiver: ActorRef
}
