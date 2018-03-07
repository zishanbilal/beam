package beam.agentsim.agents.rideHail

import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Coord
import org.matsim.core.utils.collections.QuadTree

import scala.collection.mutable

class TNCMultiIterationData(){
  val radisForForceCalculationInMeters=300;
  val maxHistoriySize=3;
  val historyDecayFactor=0.5;
  val minDurationToConsiderTNCIdleInSeconds=600;

  val intervalInSeconds=300;

  var tncHistoricData = new mutable.ListBuffer[TNCHistoryData]


  def addHistoricData(data: TNCHistoryData): Unit ={
    // TODO: refactor - use appropriate data structure
    tncHistoricData.prepend(data)
  }


  def getForceVectorAtLocation(spaceTime: SpaceTime): ForceVector = {
    var forceFactor=1.0;

    var force=ForceVector.origin // TODO: replace by empty vector
    for ( i <- 1 to tncHistoricData.size){
      force +=(getIdlingTNCForcAtLocation(i,spaceTime) + getPassengerWaitingTimeForceAtLocation(i,spaceTime))*forceFactor
      forceFactor*=historyDecayFactor
    }

    if (tncHistoricData.size>maxHistoriySize)
      tncHistoricData=tncHistoricData.dropRight(1)

    force
  }



  def getIdlingTNCForcAtLocation(iterationIndex:Int, locationTime: SpaceTime  ): ForceVector ={
    val timeFilteredWaitingEventsSet=getWaitingEventsWithStartTimeIn(tncHistoricData(iterationIndex).tncIdleTimes,locationTime.time, locationTime.time +intervalInSeconds ,true)
    val filteredWaitingEvents=getWaitingEventsInRadius(timeFilteredWaitingEventsSet,locationTime.loc,radisForForceCalculationInMeters,minDurationToConsiderTNCIdleInSeconds)

    var forceVector=ForceVector.origin

    for (waitingEvent <-filteredWaitingEvents){
      val idleTNCForceVector=new ForceVector(waitingEvent.location.loc,locationTime.loc)
      forceVector += idleTNCForceVector*(1/(idleTNCForceVector.length()*idleTNCForceVector.length()))
    }

    forceVector
    // do we need a maximum force length here?
  }



// TODO: add parameters for scaling into scaling part of idle and waiting times

  // TODO: think. experiment with conversion between meters and waiting time.

  def getPassengerWaitingTimeForceAtLocation(iterationIndex:Int, locationTime: SpaceTime): ForceVector ={
    val timeFilteredWaitingEventsSet=getWaitingEventsWithStartTimeIn(tncHistoricData(iterationIndex).passengerWaitingTimes,locationTime.time, locationTime.time +intervalInSeconds ,true)
    val filteredWaitingEvents=getWaitingEventsInRadius(timeFilteredWaitingEventsSet,locationTime.loc,radisForForceCalculationInMeters,0)


    var forceVector=ForceVector.origin

    for (waitingEvent <-filteredWaitingEvents){
      val waitingPassengerForceVector=new ForceVector(waitingEvent.location.loc,locationTime.loc)
      forceVector += waitingPassengerForceVector*waitingEvent.waitingDuration
    }

    forceVector
  }




/*
  def getPullingForceTowardsWaitingPassenger(coordPassenger,waitingTime, coordTNC): Force{
    val f:force = Force(coordTNC,coordPassenger)
    f.scale(waitingTime^2);
    // do we need a maximum force length here?
  }



*/




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



  def getWaitingEventsInRadius(waitingEvents: Set[WaitingEvent], referencePoint: Coord,  radius:Double, minRemainingWaitingDuration:Double): Set[WaitingEvent] = {

    var waitingEventSet = Set[WaitingEvent]()

    for (waitingEvent <- waitingEvents){
      if(withinRadius(waitingEvent, referencePoint, radius) && (waitingEvent.waitingDuration >= minRemainingWaitingDuration)){
        waitingEventSet += waitingEvent
      }
    }

    waitingEventSet
  }

  def withinRadius(waitingEvent: WaitingEvent, source: Coord, radius: Double):Boolean = {

    val distance = getDistance(source, waitingEvent.location.loc)
// TODO: double check, if we need to use different function from GIS lib for this
    distance <= radius
  }

  def getDistance(source: Coord, destination: Coord): Double = {
    val v1 = (destination.getX - source.getX) * (destination.getX - source.getX)
    val v2 = (destination.getY - source.getY) * (destination.getY - source.getY)
    Math.sqrt(v2 - v1)
  }

  def convertSetToQuadTree(waitingEvents: Set[WaitingEvent]): QuadTree[WaitingEvent] = {

    val (minX, maxX, minY, maxY) = waitingEvents.foldLeft((Double.MaxValue, Double.MinValue, Double.MaxValue, Double.MinValue)){case ((minX, maxX, minY, maxY), we) =>
      (Math.min(minX, we.location.loc.getX),
      Math.min(minY, we.location.loc.getY),
      Math.max(maxX, we.location.loc.getX),
      Math.max(maxY, we.location.loc.getX))
    }

    waitingEvents.foldLeft(new QuadTree[WaitingEvent](minX, minY, maxX, maxY)) { case (quadTree, we) =>
        quadTree.put(we.location.loc.getX, we.location.loc.getY, we)
        quadTree
    }
  }






}


