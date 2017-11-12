package beam.agentsim.agents

import akka.actor.FSM.Failure
import akka.actor.Props
import beam.agentsim.agents.BeamAgent._
import beam.agentsim.agents.PersonAgent.{Moving, PassengerScheduleEmptyTrigger, Waiting}
import beam.agentsim.agents.TransitDriverAgent.TransitDriverData
import beam.agentsim.agents.TriggerUtils._
import beam.agentsim.agents.modalBehaviors.DrivesVehicle
import beam.agentsim.agents.modalBehaviors.DrivesVehicle.StartLegTrigger
import beam.agentsim.agents.vehicles.BeamVehicle.{BeamVehicleIdAndRef, BecomeDriver, BecomeDriverSuccessAck}
import beam.agentsim.agents.vehicles.{BeamVehicle, PassengerSchedule}
import beam.agentsim.scheduler.TriggerWithId
import beam.sim.{BeamServices, HasServices}
import org.matsim.api.core.v01.Id
import org.matsim.vehicles.Vehicle

/**
  * BEAM
  */
object TransitDriverAgent {
  def props(services: BeamServices, transitDriverId: Id[TransitDriverAgent], vehicleIdAndRef: BeamVehicleIdAndRef, passengerSchedule: PassengerSchedule) = {
    Props(classOf[TransitDriverAgent], services, transitDriverId, vehicleIdAndRef, passengerSchedule)
  }
  case class TransitDriverData() extends BeamAgentData

  def createAgentIdFromVehicleId(transitVehicle: Id[Vehicle]) = {
    Id.create("TransitDriverAgent-" + BeamVehicle.noSpecialChars(transitVehicle.toString), classOf[TransitDriverAgent])
  }
}

class TransitDriverAgent(val beamServices: BeamServices,
                         val transitDriverId: Id[TransitDriverAgent],
                         val vehicleIdAndRef: BeamVehicleIdAndRef,
                         val initialPassengerSchedule: PassengerSchedule) extends
  BeamAgent[TransitDriverData] with HasServices with DrivesVehicle[TransitDriverData] {
  override val id: Id[TransitDriverAgent] = transitDriverId
  override val data: TransitDriverData = TransitDriverData()
  override def logPrefix(): String = s"TransitDriverAgent:$id "

  chainedWhen(Uninitialized){
    case Event(TriggerWithId(InitializeTrigger(tick),triggerId), info: BeamAgentInfo[TransitDriverData]) =>
      logDebug(s" $id has been initialized, going to Waiting state")
      holdTickAndTriggerId(tick,triggerId)
      vehicleIdAndRef.ref ! BecomeDriver(tick, id, Some(initialPassengerSchedule))
      goto(PersonAgent.Waiting)
  }

  chainedWhen(AnyState) {
    case Event(BecomeDriverSuccessAck, _) =>
      val (tick, triggerId) = releaseTickAndTriggerId()
      beamServices.schedulerRef ! completed(triggerId,schedule[StartLegTrigger](passengerSchedule.schedule.firstKey.startTime,self,passengerSchedule.schedule.firstKey))
      stay
    case Event(TriggerWithId(PassengerScheduleEmptyTrigger(tick), triggerId), _) =>
      stop replying completed(triggerId)
  }

  when(Waiting) {
    case ev@Event(_, _) =>
      handleEvent(stateName, ev)
  }
  when(Moving) {
    case ev@Event(_, _) =>
      handleEvent(stateName, ev)
  }
  when(AnyState) {
    case ev@Event(_, _) =>
      handleEvent(stateName, ev)
    case msg@_ =>
      stop(Failure(s"Unrecognized message ${msg}"))
  }
}