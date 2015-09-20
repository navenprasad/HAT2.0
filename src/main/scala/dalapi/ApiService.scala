package dalapi

import akka.actor.{ActorRefFactory, ActorLogging}
import dalapi.service._
import spray.routing.HttpServiceActor

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ApiService extends HttpServiceActor with ActorLogging {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  override def actorRefFactory = context

  // Initialise all the service the actor handles
  val helloService = new HelloService {
    def actorRefFactory = context
  }

  val dataService = new DataService {
    def actorRefFactory = context
  }

  val bundleService = new BundleService {
    override implicit def actorRefFactory: ActorRefFactory = context
  }

  val eventsService = new EventsService {
    def actorRefFactory = context
  }

  val locationsService = new LocationsService {
    def actorRefFactory = context
  }

  val peopleService = new PeopleService {
    def actorRefFactory = context
  }

  val thingsService = new ThingsService {
    def actorRefFactory = context
  }

  val organisationsService = new OrganisationsService {
    def actorRefFactory = context
  }

  // Concatenate all their handled routes
  val routes = helloService.routes ~
    dataService.routes ~
    bundleService.routes ~
    eventsService.routes ~
    locationsService.routes ~
    peopleService.routes ~
    thingsService.routes ~
    organisationsService.routes

  def receive = runRoute(routes)
}
