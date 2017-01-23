import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  import services._

  override def configure(): Unit = {
    bindActor[EventPublisherLauncher]("eventPublisherLauncher")
    bindActorFactory[EventPublisher, EventPublisherFactory]
    bind(classOf[ServicesLauncher])
  }
}
