package beam.agentsim.agents.rideHail

import org.matsim.api.core.v01.Coord

import scala.collection.mutable

class TNCMultiIterationData(){

  val radisForForceCalculation=300;



  val tncHistoricData = new mutable.ArrayBuffer[TNCHistoryData]

  def getForceAtLocation(): Unit ={

  }


}


case class Force(startCoord: Coord, endCoord: Coord)