package beam.agentsim.agents.rideHail

import akka.actor.ActorRef
import org.matsim.api.core.v01.events.Event
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.events.handler.BasicEventHandler

class TNCWaitingTimesCollector(eventsManager: EventsManager) extends BasicEventHandler {

  eventsManager.addHandler(this)

  def getTNCIdlingTimes():Set[WaitingEvent]={
    ???
  }

  def getTNCPassengerWaitingTimes():Set[WaitingEvent]={
    ???
  }

  def tellHistoryToRideHailIterationHistoryActor(rideHailIterationHistoryActorRef:ActorRef): Unit ={
    rideHailIterationHistoryActorRef ! AddTNCHistoryData(null,null)
  }

  override def handleEvent(event: Event): Unit = {

  }
}