package beam.agentsim.agents.rideHail

import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Coord

import scala.collection.mutable

class TNCMultiIterationData(){
  val radisForForceCalculation=300;
  val maxHistoriySize;
  val historyDecayFactor=0.5;

  val tncHistoricData = new mutable.ListBuffer[TNCHistoryData]

  def getForceVectorAtLocation(spaceTime: SpaceTime): ForceVector = {
    val forceFactor=1;
    for (tncHistData <-tncHistoricData){
      tncHistData.passengerWaitingTimes
    }



    waitingLocations = getPreviousIteration.AtTime(time).getWaitingLocationsInRadius(coordinate, radius);

    idlingLocations = getPreviousIteration.AtTime(time).getIdlingLocationsInRadius(coordinate, radius);

    val forces = ArrayBuffer[Force]();

    for (waitingLoc <- waitingLocations) {
      forces.add(forces);
    }

    for (waitingLoc <- idlingLocations) {
      forces.add(forces);
    }

    val finalForce = getSumOfForces(forces)
    finalForce;


  }

def getWaitingEventsInRadius(): Unit ={

}






}


case class ForceVector(startCoord: Coord, endCoord: Coord)