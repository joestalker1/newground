package services

import akka.actor.ActorRef

/**
  * Base broadcast message
  *
  */
trait Message {
   val receiver: ActorRef
}

trait EmptyMessage extends Message {
   val receiver: ActorRef = ActorRef.noSender
}
