package beam.util

import beam.sim.config.BeamConfig
import beam.sim.config.BeamConfig.Beam.Routing
import com.typesafe.config.ConfigFactory

object ConfigUtil {
  val TEST_CONFIG: String =
    """
      |routerClass = "beam.router.r5.R5RoutingWorker"
      |baseDate = "2016-10-17T00:00:00-07:00"
      |r5 {
      |  directory = "/model-inputs/r5"
      |  departureWindow = 15
      |}
      |otp {
      |  directory = "/model-inputs/otp"
      |  routerIds = ["sf"]
      |}
      |gtfs {
      |  operatorsFile = "src/main/resources/GTFSOperators.csv"
      |  outputDir = "/gtfs"
      |  apiKey = "ABC123"
      |  crs = "epsg26910"
      |}
    """.stripMargin

  def buildDefaultConfig: BeamConfig =
    BeamConfig(null, BeamConfig.Beam(null, "beam", null, null, BeamConfig.Beam.Routing(ConfigFactory.parseString(TEST_CONFIG)), null), null, null)

  def buildDefaultRouting: Routing =
    BeamConfig.Beam.Routing(ConfigFactory.parseString(TEST_CONFIG))

  def buildRoutingWithBaseDate(baseDate: String): Routing =
    BeamConfig.Beam.Routing(baseDate, null, null, null, null)



}
