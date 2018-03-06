package beam.agentsim.agents.rideHail

import java.util

import akka.actor.ActorRef
import beam.agentsim.events.{PathTraversalEvent, SpaceTime}
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.events.{Event, PersonEntersVehicleEvent}
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



  val events = new util.TreeMap[String, Event]();


  /*
    location: end.x="0.03995" end.y="0.0200499" (same as start.x="0.03995" start.y="0.02995")
		time: arrival_time="24322"
		waitingduration: departure_time="24322" - arrival_time="24322"
   */

  val listOfVehicles = new util.ArrayList[String]()
  val waitingEvents = new util.HashMap[String, util.List[WaitingEvent]]()

  override def handleEvent(event: Event): Unit = {
    if(event.isInstanceOf[PathTraversalEvent]){

      val pathTraversalEvent = event.asInstanceOf[PathTraversalEvent]

      val vehilcleId = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_VEHICLE_ID)

      val waitingDuration: Long = if(listOfVehicles.indexOf(vehilcleId) == -1){

        listOfVehicles.add(vehilcleId)
        val arrivalTime = 0
        val departureTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_DEPARTURE_TIME).toLong
        departureTime - arrivalTime

      }else{

        val arrivalTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_ARRIVAL_TIME).toLong
        val departureTime = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_DEPARTURE_TIME).toLong
        departureTime - arrivalTime
      }


      //val startX = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_START_COORDINATE_X)
      //val startY = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_START_COORDINATE_Y)

      val endX = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_END_COORDINATE_X).toDouble
      val endY = pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_END_COORDINATE_Y).toDouble


      val timeIdlingStart: Long  = if(listOfVehicles.indexOf(vehilcleId) == -1){
        0
      }else{
        pathTraversalEvent.getAttributes.get(PathTraversalEvent.ATTRIBUTE_ARRIVAL_TIME).toLong
      }

      val spaceTime = SpaceTime(new Coord(endX, endY), timeIdlingStart)

      val waitingEvent = WaitingEvent(spaceTime, waitingDuration)

      val waitingEventsList = waitingEvents.get(vehilcleId)

      if(waitingEventsList == null){

        val list = new util.ArrayList[WaitingEvent]()
        list.add(waitingEvent)
        waitingEvents.put(vehilcleId, list)

      }else {

        waitingEventsList.add(waitingEvent)
        waitingEvents.put(vehilcleId, waitingEventsList)

      }

      println("Another event added")

    }else if(event.isInstanceOf[PersonEntersVehicleEvent]){

      val personEntersVehicleEvent = event.asInstanceOf[PersonEntersVehicleEvent]


    }



  }
}