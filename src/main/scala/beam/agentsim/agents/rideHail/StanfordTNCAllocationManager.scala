package beam.agentsim.agents.rideHail
import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Id

class StanfordTNCAllocationManager extends TNCResourceAllocationManager {
  override def getNonBindingTravelProposalAsEstimate(startLocation: SpaceTime, endLocation: SpaceTime): RideHailingManager.TravelProposal = ???

  override def allocatePassengers(inquiryIds: Vector[Id[RideHailingManager.RideHailingInquiry]]): Unit = ???

  override def repositionIdleVehicles(): Unit = ???
}
