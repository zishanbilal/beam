import beam.router.r5.NetworkCoordinator.copiedNetwork

beamServices.matsimServices.getScenario.getNetwork.getLinks.values().forEach { link =>
  val linkIndex = link.getId.toString().toInt
  val edge = copiedNetwork.streetLayer.edgeStore.getCursor(linkIndex)
  val avgTime = getAverageTime(link.getId, travelTimeCalculator)
  edge.setSpeed((link.getLength/avgTime).asInstanceOf[Short])
}