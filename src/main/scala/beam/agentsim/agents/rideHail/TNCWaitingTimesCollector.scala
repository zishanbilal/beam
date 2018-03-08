package beam.agentsim.agents.rideHail

import java.util

import scala.math.Ordering.Implicits._
import scala.collection.mutable.PriorityQueue
import akka.actor.ActorRef
import beam.agentsim.events.{ModeChoiceEvent, PathTraversalEvent, SpaceTime}
import org.matsim.api.core.v01.{Coord, Id}
import org.matsim.api.core.v01.events.{Event, PersonEntersVehicleEvent}
import org.matsim.api.core.v01.network.Network
import org.matsim.api.core.v01.population.Person
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.events.handler.BasicEventHandler

class TNCWaitingTimesCollector(eventsManager: EventsManager,matsimNetwork:Network) extends BasicEventHandler {

  eventsManager.addHandler(this)

  val network=matsimNetwork

  val idlingListOfVehicles = new util.ArrayList[String]()
//  val idlingEvents = new util.HashMap[String, util.List[WaitingEvent]]()

  val idlingEventsQueue = new PriorityQueue[WaitingEvent]()(waitingEventsOrdering)


  val modeChoiceEvents = new util.HashMap[Id[Person], ModeChoiceEvent]()


  val passengerWaitingEventsQueue = new PriorityQueue[WaitingEvent]()(waitingEventsOrdering)



  ///
  def waitingEventsOrdering = new Ordering[WaitingEvent] {
    def compare(t1:WaitingEvent, t2: WaitingEvent):Int= t2.location.time compare t1.location.time
  }

  def getTNCIdlingTimes() : PriorityQueue[WaitingEvent]={
     idlingEventsQueue
  }

  /* the time between */
  def getTNCPassengerWaitingTimes() : PriorityQueue[WaitingEvent]={


    passengerWaitingEventsQueue
  }

  def tellHistoryToRideHailIterationHistoryActor(rideHailIterationHistoryActorRef:ActorRef): Unit ={
    rideHailIterationHistoryActorRef ! AddTNCHistoryData(null)
  }


  override def handleEvent(event: Event): Unit = {
    if(event.isInstanceOf[PathTraversalEvent]) {

      collectTNCIdlingTimes(event)
    }else if(event.isInstanceOf[ModeChoiceEvent]){

      // Note the event, and then wait for the PersonEntersVehicleEvent
      // Create a map with the key as the passenger id, value as a tupl

      // 1. Just maintain the list of mode choice events,
      // 2. whenever a personentersvehicleevent comes up check if a corresponding modechoice event exits.
      // 3. get that mode choice event
      // 4. perform the calculation of the waiting time using personentersvehicleevent - modechoiceevent times
      // 5. put that in the PriorityQueue
      collectModeChoiceEvents(event)
    }else if(event.isInstanceOf[PersonEntersVehicleEvent]){

      //
      checkCorrespondingModeChoiceEvent(event)
    }
  }

  def collectTNCIdlingTimes(event: Event): Unit = {
    val pathTraversalEvent = event.asInstanceOf[PathTraversalEvent]

    val vehicleId = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_VEHICLE_ID).toString

    if (vehicleId.startsWith("rideHailingVehicle")) {

      val vehilcleId = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_VEHICLE_ID)

      val waitingDuration: Long = if (idlingListOfVehicles.indexOf(vehilcleId) == -1) {

        idlingListOfVehicles.add(vehilcleId)
        val arrivalTime = 0
        val departureTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_DEPARTURE_TIME).toLong
        departureTime - arrivalTime

      } else {

        val arrivalTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_ARRIVAL_TIME).toLong
        val departureTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_DEPARTURE_TIME).toLong
        departureTime - arrivalTime
      }

      val endX = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_END_COORDINATE_X).toDouble
      val endY = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_END_COORDINATE_Y).toDouble


      val timeIdlingStart: Long = if (idlingListOfVehicles.indexOf(vehilcleId) == -1) {
        0
      } else {
        pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_ARRIVAL_TIME).toLong
      }

      val spaceTime = SpaceTime(new Coord(endX, endY), timeIdlingStart)

      val waitingEvent = WaitingEvent(spaceTime, waitingDuration)

      idlingEventsQueue.enqueue(waitingEvent)
    }
  }

  def collectModeChoiceEvents(event: Event): Unit = {

      // Add this to the list of mode choice events
    val modeChoiceEvent = event.asInstanceOf[ModeChoiceEvent]
    val personId = modeChoiceEvent.getAttributes.get(ModeChoiceEvent.ATTRIBUTE_PERSON_ID)
    if(personId.startsWith("rideHailingVehicle")) {
      modeChoiceEvents.put(modeChoiceEvent.getPersonId, modeChoiceEvent)
    }
  }

  override def reset(iteration: Int): Unit = {

    for(i <- 1 to passengerWaitingEventsQueue.size){
      println(passengerWaitingEventsQueue.dequeue().location.time)
    }

    //println("found")
  }

  def checkCorrespondingModeChoiceEvent(event: Event): Unit = {

    val personEntersVehicleEvent = event.asInstanceOf[PersonEntersVehicleEvent]
    val personId = personEntersVehicleEvent.getAttributes.get(PersonEntersVehicleEvent.ATTRIBUTE_VEHICLE).toString

    if(personId.startsWith("rideHailingVehicle")) {

      val modeChoiceEvent = modeChoiceEvents.get(personEntersVehicleEvent.getPersonId)

      if (modeChoiceEvent != null) {

        val time: Double = modeChoiceEvent.getTime
        val loc = modeChoiceEvent.getAttributes.get(ModeChoiceEvent.ATTRIBUTE_LOCATION).toString
        val waitingDuration = personEntersVehicleEvent.getTime - modeChoiceEvent.getTime

        val coord = network.getLinks.get(Id.createLinkId(loc)).getCoord

        val spaceTime = new SpaceTime(coord, time.toLong)

        val waitingEvent = WaitingEvent(spaceTime, waitingDuration)

        passengerWaitingEventsQueue.enqueue(waitingEvent)
        modeChoiceEvents.remove(modeChoiceEvent.getPersonId)
      }
    }
  }
}