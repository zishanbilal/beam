package beam.agentsim.agents.rideHail

import akka.actor.Actor
import org.matsim.api.core.v01.events.Event
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.events.handler.BasicEventHandler




class RideHailIterationHistoryActor extends Actor{
  import scala.collection.mutable;

  // TODO: optimize memory after things work/stabilized
  val tncHistoricData = new mutable.ArrayBuffer[TNCHistoryData]

  def receive = {
    case tncHistoricData: AddTNCHistoryData =>
      this.tncHistoricData += tncHistoricData.data
    case GetWaitingTimes() =>   // received message from RideHailManager
      sender() ! UpdateHistoricWaitingTimes(this.tncHistoricData.last)
    case _      =>  ???
  }
}


case class AddTNCHistoryData(data:TNCHistoryData)


case class TNCHistoryData(tncIdleTimes: Set[WaitingEvent], passengerWaitingTimes:Set[WaitingEvent])

case class GetWaitingTimes()


case class UpdateHistoricWaitingTimes(tncHistoricData: TNCHistoryData)