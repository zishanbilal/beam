package beam.router.r5;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import beam.router.BeamRouter;
import beam.router.Modes;
import beam.router.RoutingModel;
import beam.sim.BeamServices;
import beam.sim.config.BeamConfig;
import beam.sim.config.BeamConfig$;
import beam.util.ConfigUtil;
import com.conveyal.r5.streets.StreetLayer;
import com.conveyal.r5.transit.TransportNetwork;
import com.typesafe.config.ConfigFactory;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import scala.collection.JavaConverters;
import scala.collection.convert.AsScalaConverters;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorBuilder;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * @author Ahmar Nadeem
 */
public class R5RoutingWorkerTest {

    /*********************
     START - TEST DATA
     ********************/
    private static final double FROM_LONGITUDE = 40.689823;//X
    private static final double FROM_LATITUDE = -73.832639;//Y

    private static final double TO_LONGITUDE = 40.672770;//X
    private static final double TO_LATITUDE = -73.895639;//Y

    private static final String BASE_DATE = "2016-10-17T00:00:00-07:00";

    private static final String TEST_CONFIG = "routerClass = \"beam.router.r5.R5RoutingWorker\""
            + "\nbaseDate = \"2016-10-17T00:00:00-07:00\""
            + "\nr5 {\n  directory = \"/model-inputs/r5\""
            + "\n  departureWindow = 15\n}"
            + "\notp {\n  directory = \"/model-inputs/otp\"\n  routerIds = [\"sf\"]\n}"
            + "\ngtfs {\n  operatorsFile = \"src/main/resources/GTFSOperators.csv\""
            + "\n  outputDir = \"/gtfs\""
            + "\n  apiKey = \"ABC123\""
            + "\n  crs = \"epsg26910\""
            + "\n}";
    /****************
     END - TEST DATA
     ****************/

    private static BeamServices beamServices;
    private static TransportNetwork transportNetwork;
    private static ActorSystem system;
    private static R5RoutingWorker actor;

    /**
     * @Description: This function is used to setup the data to be used by test functions at class startup time.
     */
    @BeforeClass
    public static void setup() {

//        beamServices = mock(BeamServices.class);
        beamServices = mock(BeamServices.class);

        system = ActorSystem.create();
        final Props props = R5RoutingWorker.props(beamServices);
        final TestActorRef<R5RoutingWorker> ref = TestActorRef.create(system, props, "calcRoute");
        actor = ref.underlyingActor();

        transportNetwork = mock(TransportNetwork.class);
        R5RoutingWorker.transportNetwork_$eq(transportNetwork);
    }

    /**
     * Test the calcRoute function in the R5RoutingWorker class.
     */
    @Test
    public void testCalculateRoute() {

//        BeamConfig.Beam.Routing routing = new BeamConfig.Beam.Routing(baseDate, null, null, null, null);
        StreetLayer streetLayer = new StreetLayer(null);
        when(transportNetwork.streetLayer).thenReturn(streetLayer);

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);
        when(transportNetwork.getTimeZone()).thenReturn(ZoneId.systemDefault());

        Coord fromCoord = new Coord(FROM_LONGITUDE, FROM_LATITUDE);
        Facility fromFacility = new FakeFacility(fromCoord);

        Coord toCoord = new Coord(TO_LONGITUDE, TO_LATITUDE);
        Facility toFacility = new FakeFacility(toCoord);

//        RoutingModel.BeamTime departureTime = new RoutingModel.DiscreteTime(calculateTimeFromBase());

        RoutingModel.BeamTime departureTime = new RoutingModel.WindowTime(calculateTimeFromBase(), 30);

//        Vector<Modes.BeamMode> modes =  new Vector(0,1,0);
        VectorBuilder <Modes.BeamMode> modeBuilder = new VectorBuilder<>();
        modeBuilder.$plus$eq((Modes.BeamMode)Modes.BeamMode$.MODULE$.withValue("walk"));
        Vector<Modes.BeamMode> modes = modeBuilder.result();
//        Modes.BeamMode mode = Modes.BeamMode.WAITING;
//        modes = modes Modes.BeamMode.WAITING;

        Person person = null;

        BeamRouter.RoutingResponse response = actor.calcRoute(fromFacility, toFacility, departureTime, modes, person);
        assertTrue(response != null);
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system, null, true);
        system = null;
    }

    //TODO (Ahmar) move this function to a util class and parameterize it.

    /**
     * @return difference in seconds from the base time.
     * @author Ahmar Nadeem
     */
    private int calculateTimeFromBase() {

        DateTime baseTime = DateTime.parse(BASE_DATE);
        DateTime currentTime = DateTime.now();

        return Seconds.secondsBetween(baseTime, currentTime).getSeconds();
    }
}
