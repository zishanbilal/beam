package beam.router.r5;

import static org.junit.Assert.*;
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
import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.*;
import com.conveyal.r5.point_to_point.builder.TNBuilderConfig;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.profile.StreetMode;
import com.conveyal.r5.streets.Split;
import com.conveyal.r5.streets.StreetLayer;
import com.conveyal.r5.transit.TransportNetwork;
import com.typesafe.config.ConfigFactory;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.distance3d.AxisPlaneCoordinateSequence;
import org.geotools.geometry.jts.CurvedGeometryFactory;
import org.jaitools.jts.CoordinateSequence2D;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import scala.collection.JavaConverters;
import scala.collection.convert.AsScalaConverters;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private static final String ZONE_ID = "-07:00";
    private static final String TIME = "T00:00:00";
    private static final String BASE_DATE = "2016-10-17";

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
        beamServices = mock(BeamServices.class);
        system = ActorSystem.create();
        final Props props = R5RoutingWorker.props(beamServices);
        final TestActorRef<R5RoutingWorker> ref = TestActorRef.create(system, props, "calcRoute");
        actor = ref.underlyingActor();

        transportNetwork = mock(TransportNetwork.class);
        R5RoutingWorker.transportNetwork_$eq(transportNetwork);
    }

    @Test
    public void testBuildRequest(){

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);
        when(transportNetwork.getTimeZone()).thenReturn(ZoneId.systemDefault());

        Coord fromCoord = new Coord(FROM_LONGITUDE, FROM_LATITUDE);
        Facility fromFacility = new FakeFacility(fromCoord);

        Coord toCoord = new Coord(TO_LONGITUDE, TO_LATITUDE);
        Facility toFacility = new FakeFacility(toCoord);
        RoutingModel.BeamTime departureTime = new RoutingModel.WindowTime(calculateTimeFromBase(), 30);

        VectorBuilder <Modes.BeamMode> modeBuilder = new VectorBuilder<>();
        modeBuilder.$plus$eq((Modes.BeamMode)Modes.BeamMode$.MODULE$.withValue("walk"));
        Vector<Modes.BeamMode> modes = modeBuilder.result();

        ProfileRequest request = actor.buildRequest(fromFacility, toFacility, departureTime, modes);
        assertNotNull(request);
        assertFalse(request.isProfile());
        assertEquals(4, request.bikeTrafficStress);
        assertEquals(4.0, request.bikeSpeed, 0.0 );
        assertEquals(FROM_LATITUDE, request.fromLat,0.0);
        assertEquals(FROM_LONGITUDE, request.fromLon,0.0);
        assertEquals(TO_LATITUDE, request.toLat,0.0);
        assertEquals(TO_LONGITUDE, request.toLon, 0.0);
        assertEquals(1.3, request.walkSpeed, 0.3);
        assertEquals(11.0, request.carSpeed,0.0);
        assertEquals(60, request.streetTime);
        assertEquals(30, request.maxWalkTime);
        assertEquals(30, request.maxBikeTime);
        assertEquals(30, request.maxCarTime);
        assertEquals(5, request.minBikeTime);
        assertEquals(5, request.minCarTime);
        assertEquals(convertStringToDate(), request.date);
        assertEquals(0, request.limit);
        assertEquals(1, request.directModes.size());
        assertTrue(request.directModes.contains(LegMode.WALK));
        assertEquals(0.5, request.reachabilityThreshold, 0.0);
        assertEquals(0, request.bikeSafe);
        assertEquals(0, request.bikeSlope);
        assertEquals(0, request.bikeTime);
        assertEquals(5, request.suboptimalMinutes);
        assertEquals(240, request.maxTripDurationMinutes);
        assertEquals(ZoneId.systemDefault(), request.zoneId);
        assertFalse(request.wheelchair);
        assertEquals(-1, request.maxFare);
        assertEquals(220, request.monteCarloDraws);
    }

    @Test
    public void testBuildResponse(){

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);

        ProfileResponse plan =  new ProfileResponse();
        List<ProfileOption> options = new ArrayList<ProfileOption>();
        ProfileOption option = new ProfileOption();
        List<TransitJourneyID> transitJourneyIds = new ArrayList<TransitJourneyID>();
        TransitJourneyID transitJourneyId = new TransitJourneyID(1,2);
        transitJourneyIds.add(transitJourneyId);
        option.addItineraries(transitJourneyIds, ZoneId.systemDefault());
        List<Itinerary> itineraries = new ArrayList<Itinerary>();
        Itinerary itinerary = new Itinerary();
        int accessIndex = 0;
        int egressIndex = 0;
        PointToPointConnection p2pConnection = new PointToPointConnection(accessIndex, egressIndex, transitJourneyIds);
        itinerary.connection = p2pConnection;
        itinerary.startTime = ZonedDateTime.now();
        itineraries.add(itinerary);
        option.itinerary = itineraries;

        List<StreetSegment> accessList = new ArrayList<StreetSegment>();
        StreetSegment accessSegment = new StreetSegment();
        accessSegment.mode = LegMode.WALK;
        accessSegment.duration = 15;
        List<StreetEdgeInfo> streetEdges = new ArrayList<StreetEdgeInfo>();
        StreetEdgeInfo streetEdge = new StreetEdgeInfo();
        streetEdge.mode = NonTransitMode.WALK;
        streetEdge.edgeId = 5;
        double x = 1.0;
        double y = 1.0;
        double z = 1.0;
        CoordinateSequence points = new CoordinateSequence2D(3);
        GeometryFactory factory = new CurvedGeometryFactory(accessIndex);
        LineString geom = new LineString(points, factory);
        streetEdge.geometry = geom;
        streetEdges.add(streetEdge);
        accessSegment.streetEdges = streetEdges;
        accessList.add(accessSegment);
        option.access = accessList;

        List<StreetSegment> egressList = new ArrayList<StreetSegment>();
        StreetSegment egress = new StreetSegment();
        egress.mode = LegMode.WALK;
        egressList.add(egress);
        option.egress = egressList;

        options.add(option);
        plan.options = options;
        BeamRouter.RoutingResponse response = actor.buildResponse(plan);
        assertNotNull(response);
    }


    /**
     * Test the calcRoute function in the R5RoutingWorker class.
     */
    @Ignore
    @Test
    public void testCalculateRoute() {

//        BeamConfig.Beam.Routing routing = new BeamConfig.Beam.Routing(baseDate, null, null, null, null);

//        when(transportNetwork.streetLayer).thenReturn(streetLayer);

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

        DateTime baseTime = DateTime.parse(BASE_DATE+TIME+ZONE_ID);
        DateTime currentTime = DateTime.now();

        return Seconds.secondsBetween(baseTime, currentTime).getSeconds();
    }

    private LocalDate convertStringToDate(){
        return LocalDate.parse(BASE_DATE);
    }
}
