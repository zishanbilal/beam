package beam.physsim.jdeqsim.akka

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, UntypedActor}
import beam.utils.DebugLib
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.network.Network
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup
import org.matsim.core.events.EventsManagerImpl
import org.matsim.core.events.handler.EventHandler
import org.matsim.core.trafficmonitoring.TravelTimeCalculator
import java.util

import akka.event.LoggingReceive


object EventManagerActor {
  val LAST_MESSAGE = "lastMessage"
  def REGISTER_JDEQSIM_REF = "registerJDEQSimREF"

  def props(network: Network): Props = Props.create(classOf[EventManagerActor], network)
}

class EventManagerActor(var network: Network) extends Actor with Stash with ActorLogging {

  resetEventsActor()
  var jdeqsimActorREF: ActorRef = _
  var travelTimeCalculator: TravelTimeCalculator = _
  var eventsManager: EventsManager = _

  private def resetEventsActor(): Unit = {
    eventsManager = new EventsManagerImpl
    val ttccg = new TravelTimeCalculatorConfigGroup
    travelTimeCalculator = new TravelTimeCalculator(network, ttccg)
    eventsManager.addHandler(travelTimeCalculator)
  }

  override def receive = LoggingReceive{

    case event: Event => eventsManager.processEvent(event)
    case s: String => {
      if (s.equalsIgnoreCase(EventManagerActor.LAST_MESSAGE)) {
        jdeqsimActorREF.tell(travelTimeCalculator, self)
        resetEventsActor()
      }
      else if (s.equalsIgnoreCase(EventManagerActor.REGISTER_JDEQSIM_REF)) jdeqsimActorREF = sender
      else DebugLib.stopSystemAndReportUnknownMessageType()
    }
    case _ => DebugLib.stopSystemAndReportUnknownMessageType()
  }
}
