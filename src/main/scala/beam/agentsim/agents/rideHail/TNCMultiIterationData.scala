package beam.agentsim.agents.rideHail

import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Coord
import org.matsim.core.utils.collections.QuadTree

import scala.collection.mutable

class TNCMultiIterationData(){
  val radisForForceCalculation=300;
  val maxHistoriySize=3;
  val historyDecayFactor=0.5;
  val minDurationToConsiderTNCIdleInSeconds=600;

  var tncHistoricData = new mutable.ListBuffer[TNCHistoryData]


  def addHistoricData(data: TNCHistoryData): Unit ={
    // TODO: refactor - use appropriate data structure
    tncHistoricData.prepend(data)
  }


  def getForceVectorAtLocation(spaceTime: SpaceTime): ForceVector = {
    var forceFactor=1.0;

    var force=ForceVector.origin // TODO: replace by empty vector
    for ( i <- 1 to tncHistoricData.size){
      force +=(getIdlingTNCForcAtLocation(i,spaceTime) + getPassengerWaitingTimeForcAtLocation(i,spaceTime))*forceFactor
      forceFactor*=historyDecayFactor
    }

    if (tncHistoricData.size>maxHistoriySize)
      tncHistoricData=tncHistoricData.dropRight(1)

    force
  }



  def getIdlingTNCForcAtLocation(iterationIndex:Int, location: SpaceTime  ): ForceVector ={
    null
  }

  def getPassengerWaitingTimeForcAtLocation(iterationIndex:Int, location: SpaceTime  ): ForceVector ={
    null
  }




  def getWaitingEventsWithStartTimeIn(waitingEvents: mutable.PriorityQueue[WaitingEvent], startTime: Double, endTime: Double, keepStillWaiting: Boolean): Set[WaitingEvent] ={

    var waitingEventSet = Set[WaitingEvent]()

    for (i <- 1 to waitingEvents.size){

      val waitingEvent = waitingEvents.dequeue()

      // TODO The condition on keepStillWaiting needs to be added here
      if((waitingEvent.location.time <= startTime && (waitingEvent.location.time <= endTime))){
        waitingEventSet += waitingEvent
      }
    }

    waitingEventSet
  }



  def getWaitingEventsInRadius(waitingEvents: mutable.PriorityQueue[WaitingEvent], referencePoint: Coord,  radius:Double, endTime: Double, minRemainingWaitingDuration:Double): Set[WaitingEvent] = {

    var waitingEventSet = Set[WaitingEvent]()

    for (i <- 1 to waitingEvents.size){
      val waitingEvent = waitingEvents.dequeue()

      if(withinRadius(waitingEvent, referencePoint, radius) && (waitingEvent.location.time <= endTime) && (waitingEvent.waitingDuration >= minRemainingWaitingDuration)){
        waitingEventSet += waitingEvent
      }
    }

    waitingEventSet
  }

  def withinRadius(waitingEvent: WaitingEvent, source: Coord, radius: Double):Boolean = {

    val distance = getDistance(source, waitingEvent.location.loc)

    distance <= radius
  }

  def getDistance(source: Coord, destination: Coord): Double = {
    val v1 = (destination.getX - source.getX) * (destination.getX - source.getX)
    val v2 = (destination.getY - source.getY) * (destination.getY - source.getY)
    Math.sqrt(v2 - v1)
  }

  def convertSetToQuadTree(waitingEvents: Set[WaitingEvent]): QuadTree[WaitingEvent] ={
    null
  }






}


