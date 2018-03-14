package beam.agentsim.agents.rideHail
import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Id

class TNCDefaultResourceAllocationManager extends TNCResourceAllocationManager {
  override def getNonBindingTravelProposalAsEstimate(startLocation: SpaceTime, endLocation: SpaceTime): RideHailingManager.TravelProposal = ???



  override def repositionIdleVehicles(): Unit = {}

  override def allocatePassengers(inquiryIds: Vector[Id[RideHailingManager.RideHailingInquiry]]): Unit = {}
}
