import beam.router.r5.NetworkCoordinator.transportNetwork

// insert this in NetworkCoordinator.loadNetwork (At the end, when transportNetwork initialized)
val a=transportNetwork.streetLayer.edgeStore
println("streets")
for (i<- 0 to transportNetwork.streetLayer.edgeStore.nEdges()-1) {
  val cursor = transportNetwork.streetLayer.edgeStore.getCursor(i)

  println(i +" -> " + cursor)
}