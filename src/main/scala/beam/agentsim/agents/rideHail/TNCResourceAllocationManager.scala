package beam.agentsim.agents.rideHail

import beam.agentsim.agents.rideHail.RideHailingManager._
import beam.agentsim.events.SpaceTime
import org.matsim.api.core.v01.Id

trait TNCResourceAllocationManager {

  // all methods can use RHM.getFleetInfo

  def getNonBindingTravelProposalAsEstimate(startLocation:SpaceTime, endLocation:SpaceTime):TravelProposal // we take over current implementation
    // TODO: also give back
     // can use RHM.getEstimatedLinkTravelTimesWithAdditionalLoad


  def allocatePassengers(inquiryIds: Vector[Id[RideHailingInquiry]]) // input: information of batch of tnc requests (from
    // use RHM.assignTNC

  def repositionIdleVehicles()

    // use RHM.moveIdleTNCTo to implement

}

object TNCResourceAllocationManager{
  val DEFAULT_MANAGER="DEFAULT_ALLOCATION_MANAGER"
  val STANFORD_ALLOCATION_MANAGER_V1="STANFORD_ALLOCATION_MANAGER_V1"
}
