// do at the end of network loading of network coordinator

for (i<- 0 to transportNetwork.streetLayer.edgeStore.nEdges()-1) {
  val cursor = transportNetwork.streetLayer.edgeStore.getCursor(i)

  println(i +"\t" + cursor.getGeometry.getCoordinate.x  +"\t" + cursor.getGeometry.getCoordinate.y +"\t" + cursor.getLengthM)
}
print()