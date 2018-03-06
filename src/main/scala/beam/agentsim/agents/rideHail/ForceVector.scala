package beam.agentsim.agents.rideHail


import org.matsim.api.core.v01.Coord

class ForceVector(startCoord: Coord, var endCoord: Coord) {


  def *(factor: Double): ForceVector = {
    val newEndCoord = new Coord(startCoord.getX + deltaX * factor, startCoord.getY + deltaY * factor)
    new ForceVector(startCoord, newEndCoord)
  }

  def deltaX(): Double = endCoord.getX - startCoord.getX

  def deltaY(): Double = endCoord.getY - startCoord.getY

  def +(other: ForceVector): ForceVector = {
    val newEndCoord = new Coord(endCoord.getX() + other.deltaX(), endCoord.getY() + other.deltaY())
    new ForceVector(startCoord, newEndCoord)
  }

  def length(): Double = {
    Math.sqrt(deltaX()*deltaX()+deltaY()*deltaY())
  }


}

object  ForceVector{

  val origin:ForceVector=new ForceVector(new Coord(0,0), new Coord(0,0))

}