package beam.agentsim.routing.opentripplanner

import java.io.File
import java.time.ZonedDateTime
import java.util
import java.util.Locale

import akka.actor.Props
import beam.agentsim.routing.opentripplanner.OpenTripPlannerRouter._
import beam.agentsim.routing.{BeamRouter, RoutingRequest}
import beam.agentsim.sim.AgentsimServices
import org.geotools.referencing.CRS
import org.matsim.api.core.v01.population.{Person, PlanElement}
import org.matsim.facilities.Facility
import org.matsim.utils.objectattributes.attributable.Attributes
import org.opengis.referencing.operation.MathTransform
import org.opentripplanner.common.model.GenericLocation
import org.opentripplanner.graph_builder.GraphBuilder
import org.opentripplanner.routing.core.{State, TraverseMode}
import org.opentripplanner.routing.impl._
import org.opentripplanner.routing.services.GraphService
import org.opentripplanner.routing.spt.GraphPath
import org.opentripplanner.standalone.{CommandLineParameters, Router}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

/**
  */
class OpenTripPlannerRouter (agentsimServices: AgentsimServices) extends BeamRouter {
  import beam.agentsim.sim.AgentsimServices._

  val log: Logger = LoggerFactory.getLogger(getClass)
  val baseDirectory: File = new File(beamConfig.beam.sim.sharedInputs + beamConfig.beam.routing.otp.directory)
  val routerIds: List[String] = beamConfig.beam.routing.otp.routerIds
  val graphService: GraphService = makeGraphService()
  val router: Router = graphService.getRouter(routerIds.head)
  val transform: MathTransform = CRS.findMathTransform(CRS.decode("EPSG:26910",true),CRS.decode("EPSG:4326",true),false)

  def calcRoute(fromFacility: Facility[_], toFacility: Facility[_], departureTime: Double, person: Person): java.util.LinkedList[PlanElement] = {
      val request = new org.opentripplanner.routing.core.RoutingRequest()
      request.routerId = routerIds.head
      request.addMode(TraverseMode.CAR)
      /*
      val fromPt: DirectPosition2D = new DirectPosition2D(fromFacility.getCoord.getX,fromFacility.getCoord.getY)
      var fromProjected: DirectPosition2D = new DirectPosition2D(0,0)
      val toPt: DirectPosition2D = new DirectPosition2D(toFacility.getCoord.getX,toFacility.getCoord.getY)
      var toProjected: DirectPosition2D = new DirectPosition2D(0,0)
      transform.transform(fromPt,fromProjected)
      transform.transform(toPt,toProjected)
      request.from = new GenericLocation(fromProjected.y,fromProjected.x)
      request.to = new GenericLocation(toProjected.y,toProjected.x)
      */
      request.from = new GenericLocation(fromFacility.getCoord.getY,fromFacility.getCoord.getX)
      request.to = new GenericLocation(toFacility.getCoord.getY,toFacility.getCoord.getX)
      request.dateTime = ZonedDateTime.now().minusMonths(5).toEpochSecond
      request.maxWalkDistance = 804.672
      request.locale = Locale.ENGLISH
      val gpFinder = new GraphPathFinder(router)
      //TODO this is not robust to OTP exceptions
      val paths = gpFinder.graphPathFinderEntryPoint(request)
      val beamTrips = for(path: GraphPath <- paths.asScala.toVector) yield {
          val verticesModesTimes: Vector[(String, String, Long)] = for (state: State <- path.states.asScala.toVector) yield {
            val theMode : String = if (state.getNonTransitMode == null) { state.getTripId.getAgencyId } else { state.getNonTransitMode.name() }
            Tuple3(state.getVertex.getLabel,theMode,state.getTimeSeconds)
          }
          val it = verticesModesTimes.iterator
          var activeTuple = it.next()
          var activeGraphPath = Vector[String](activeTuple._1)
          var activeMode = activeTuple._2
          var activeStart = activeTuple._3
          var beamLegs = Vector[BeamLeg]()
          while (it.hasNext) {
            activeTuple = it.next()
            if (activeTuple._2 == activeMode) {
              activeGraphPath = activeGraphPath :+ activeTuple._1
            } else {
              beamLegs = beamLegs :+ BeamLeg(activeStart, activeMode, BeamGraphPath(activeGraphPath))
              activeMode = activeTuple._2
              activeStart = activeTuple._3
            }
          }
          beamLegs = beamLegs :+ BeamLeg(activeStart, activeMode, BeamGraphPath(activeGraphPath))
          BeamTrip(beamLegs)
      }
      val planElementList = new java.util.LinkedList[PlanElement]()
      planElementList.add(BeamItinerary(beamTrips))
      planElementList
  }

  override def receive: Receive = {
    case RoutingRequest(fromFacility, toFacility, departureTime, personId) =>
      log.info(s"OTP Router received routing request from $sender")
      val person: Person = agentsimServices.matsimServices.getScenario.getPopulation.getPersons.get(personId)
      val senderRef = sender()
      val plans = calcRoute(fromFacility, toFacility, departureTime, person)
      senderRef ! RoutingResponse(plans)
//        sender() ! calcRoute(fromFacility, toFacility, departureTime, person)
    case msg =>
      log.info(s"unknown message received by OTPRouter $msg")
  }

  private def makeGraphService(): GraphService = {
    log.info("Loading graph..")

    val graphService = new GraphService()
    graphService.graphSourceFactory = new InputStreamGraphSource.FileFactory(baseDirectory)

    val params = makeParams()

    buildAndPersistGraph(graphService, params)

    if (routerIds != null && routerIds.nonEmpty) {
      val graphScanner = new GraphScanner(graphService, params.graphDirectory, params.autoScan)
      graphScanner.basePath = params.graphDirectory
      graphScanner.defaultRouterId = routerIds.head
      graphScanner.autoRegister = routerIds.asJava
      graphScanner.startup()
    }

    log.info("Graph loaded successfully")

    graphService
  }

  private def makeParams(): CommandLineParameters = {
    val params = new CommandLineParameters
    params.basePath = baseDirectory.getAbsolutePath
    params.port = 338080
    params.securePort = 338081
    params.routerIds = routerIds.asJava
    params.infer()
    params.autoReload = false
    params
  }

  private def buildAndPersistGraph(graphService: GraphService, params: CommandLineParameters): Unit = {
    routerIds.foreach(routerId => {
      val graphDirectory = new File(s"${baseDirectory.getAbsolutePath}/graphs/$routerId/")
      val graphBuilder = GraphBuilder.forDirectory(params, graphDirectory)
      graphBuilder.setAlwaysRebuild(false)
      if (graphBuilder != null) {
        graphBuilder.run()
        val graph = graphBuilder.getGraph
        graph.index(new DefaultStreetVertexIndexFactory)
        graphService.registerGraph("", new MemoryGraphSource("", graph))
      }
    })
  }


}

object OpenTripPlannerRouter {
  def props(agentsimServices: AgentsimServices) = Props(classOf[OpenTripPlannerRouter],agentsimServices)

  case class RoutingResponse(els: util.LinkedList[PlanElement])

  case class BeamItinerary(itinerary: Vector[BeamTrip]) extends PlanElement {
    override def getAttributes: Attributes = new Attributes()
  }
  case class BeamTrip(legs: Vector[BeamLeg])
  case class BeamLeg(startTime: Long, mode: String, graphPath: BeamGraphPath)
  case class BeamGraphPath(value: Vector[String])
}