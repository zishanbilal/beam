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

  val tncHistoricData = new mutable.ListBuffer[TNCHistoryData]


  def addHistoricData(data: TNCHistoryData): Unit ={
    // TODO: refactor - use appropriate data structure
    tncHistoricData.insert(0,data)
  }


  def getForceVectorAtLocation(spaceTime: SpaceTime): ForceVector = {
    var forceFactor=1;

    var force=null // TODO: replace by empty vector
    for ( i <- 1 to tncHistoricData.size){
      force +=(getIdlingTNCForcAtLocation(i,spaceTime) + getPassengerWaitingTimeForcAtLocation(i,spaceTime))*forceFactor
      forceFactor*=historyDecayFactor
    }

    if (tncHistoricData.size>maxHistoriySize)
    tncHistoricData.dropRight(1)

    force
  }



  def getIdlingTNCForcAtLocation(iterationIndex:Int, location: SpaceTime  ): ForceVector ={
    null
  }

  def getPassengerWaitingTimeForcAtLocation(iterationIndex:Int, location: SpaceTime  ): ForceVector ={
    null
  }




  def getWaitingEventsWithStartTimeIn(waitingEvents: mutable.PriorityQueue[WaitingEvent], startTime: Double, endTime: Double, keepStillWaiting: Boolean): Set[WaitingEvent] ={




    null
  }



def getWaitingEventsInRadius(waitingEvents: Set[WaitingEvent], radius:Double, endTime: Double, minRemainingWaitingDuration:Double): Set[WaitingEvent] ={
  //convertSetToQuadTree(waitingEvents)


  null
}

  def convertSetToQuadTree(waitingEvents: Set[WaitingEvent]): QuadTree[WaitingEvent] ={
    null
  }






}


class ForceVector(startCoord: Coord, endCoord: Coord){

  def *(factor: Double): ForceVector ={
    null
  }

  def +(other: ForceVector): ForceVector ={
    null
  }


}