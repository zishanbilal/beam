package beam.agentsim.agents.rideHail

import java.util

import scala.math.Ordering.Implicits._
import scala.collection.mutable.PriorityQueue

import akka.actor.ActorRef
import beam.agentsim.events.{PathTraversalEvent, SpaceTime}
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.events.{Event, PersonEntersVehicleEvent}
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.events.handler.BasicEventHandler

class TNCWaitingTimesCollector(eventsManager: EventsManager) extends BasicEventHandler {


  eventsManager.addHandler(this)

  val events = new util.TreeMap[String, Event]();
  val listOfVehicles = new util.ArrayList[String]()
  val waitingEvents = new util.HashMap[String, util.List[WaitingEvent]]()



  def waitingEventsOrdering = new Ordering[WaitingEvent] {
    def compare(t1:WaitingEvent, t2: WaitingEvent):Int= t2.location.time compare t1.location.time
  }

  def getTNCIdlingTimes() : PriorityQueue[WaitingEvent]={
    val weventPriorityQueue = new PriorityQueue[WaitingEvent]()(waitingEventsOrdering)

    waitingEvents.forEach {
      case(vid : String, weventList: util.List[WaitingEvent] ) => {
        weventList.forEach{
          case(wevent: WaitingEvent) => {
            weventPriorityQueue.enqueue(wevent)
          }
        }
      }
    }

    weventPriorityQueue
  }

  def getTNCPassengerWaitingTimes() : Set[WaitingEvent]={

    ???
  }

  def tellHistoryToRideHailIterationHistoryActor(rideHailIterationHistoryActorRef:ActorRef): Unit ={
    rideHailIterationHistoryActorRef ! AddTNCHistoryData(null,null)
  }


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
    }
  }
}