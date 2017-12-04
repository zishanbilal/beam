package beam.integration



import java.util
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import beam.agentsim.events.PathTraversalEvent
import beam.agentsim.scheduler.BeamAgentScheduler
import beam.agentsim.scheduler.BeamAgentScheduler.SchedulerProps
import beam.physsim.jdeqsim.AgentSimToPhysSimPlanConverter
import beam.physsim.jdeqsim.akka.{AkkaEventHandlerAdapter, EventManagerActor, JDEQSimActor}
import beam.router.BeamRouter.UpdateTravelTime
import beam.sim._
import beam.sim.config.{BeamConfig, BeamLoggingSetup, ConfigModule}
import beam.sim.controler.BeamControler
import beam.sim.controler.corelisteners.{BeamControllerCoreListenersModule, BeamPrepareForSimImpl}
import beam.sim.modules.{AgentsimModule, BeamAgentModule, UtilsModule}
import beam.utils.{AkkaMessageLogger, AkkaDebugMessage, FileUtils}
import beam.utils.reflection.ReflectionUtils
import com.conveyal.r5.streets.StreetLayer
import glokka.Registry
import glokka.Registry.Created
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.population._
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.config.{Config, ConfigUtils}
import org.matsim.core.controler.events.{IterationEndsEvent, IterationStartsEvent, ShutdownEvent}
import org.matsim.core.controler.listener.{IterationEndsListener, IterationStartsListener, ShutdownListener}
import org.matsim.core.controler.{AbstractModule, ControlerI, NewControlerModule, PrepareForSim}
import org.matsim.core.events.EventsUtils
import org.matsim.core.events.handler.{BasicEventHandler, EventHandler}
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup
import org.matsim.core.population.routes.RouteUtils
import org.matsim.core.scenario.{ScenarioByInstanceModule, ScenarioUtils}
import org.matsim.core.utils.collections.Tuple
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpecLike}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await

class RouterTest
  extends TestKit(ActorSystem("beam-actor-system"))
    //extends RunBeam
    with RunBeam
    with MustMatchers with WordSpecLike with ImplicitSender with MockitoSugar
    with ShutdownListener with IterationEndsListener{

  val __this_instance = this

  var services: BeamServices = _
  val eventsManager: EventsManager = EventsUtils.createEventsManager()
  /////////////////////////
  // These class variables need to be in place before runBeamWithConfigFile method is called
  implicit val timeout = Timeout(50000, TimeUnit.SECONDS)
  val log = LoggerFactory.getLogger(classOf[RouterTest])
  ////////////////////////////////

  override def beamInjector(scenario: Scenario,  matSimConfig: Config,mBeamConfig: Option[BeamConfig] = None): com.google.inject.Injector =
    org.matsim.core.controler.Injector.createInjector(matSimConfig, AbstractModule.`override`(ListBuffer(new AbstractModule() {

      override def install(): Unit = {
        // MATSim defaults
        install(new NewControlerModule)
        install(new ScenarioByInstanceModule(scenario))
        install(new controler.ControlerDefaultsModule)
        install(new BeamControllerCoreListenersModule)

        // Beam Inject below:
        install(new ConfigModule)
        install(new AgentsimModule)
        install(new BeamAgentModule)
        install(new UtilsModule)
      }
    }).asJava, new AbstractModule() {
      override def install(): Unit = {
        // Override MATSim Defaults
        bind(classOf[PrepareForSim]).to(classOf[BeamPrepareForSimImpl])
        // Beam -> MATSim Wirings
        bindMobsim().to(classOf[BeamMobsim]) //TODO: This will change

        addControlerListenerBinding().toInstance(__this_instance)
        addControlerListenerBinding().to(classOf[BeamSim])

        bind(classOf[EventsManager]).toInstance(eventsManager)
        bind(classOf[ControlerI]).to(classOf[BeamControler]).asEagerSingleton()
        mBeamConfig.foreach(beamConfig => bind(classOf[BeamConfig]).toInstance(beamConfig)) //Used for testing - if none passed, app will use factory BeamConfig


      }
    }))
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  override def notifyShutdown(event: ShutdownEvent): Unit = {

    testRouterMessages
  }

  override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
    //testRouterMessages
  }
  //

  def testRouterMessages = {
    implicit val timeout = Timeout(50000, TimeUnit.SECONDS)

    try {

      val messageCollector = system.actorOf(Props(classOf[AkkaMessageLogger]))
      println("messageCollector - From RouterTest -> " + messageCollector.path)

      // Test cases
      messageCollector.tell(AkkaMessageLogger.GetList("startPhyssim"), testActor)

      "AkkaMessageLogger" must {
        "return a list of debug messages " in {
          within(50000 millis) {
            expectMsgPF() {
              case l: mutable.MutableList[AkkaDebugMessage] => {

                //log.info("DebugMessage List Received")
                println("DebugMessage List Received: " + l.size)
                AkkaMessageLogger.printList(l)

              }
              case o: Any => {
                println(o)
              }
            }
          }
        }
      }

    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  //
  rumBeamWithConfigFile(Option("test/input/beamville/beam.conf"))
}

