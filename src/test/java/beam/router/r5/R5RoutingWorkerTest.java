package beam.router.r5;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import beam.router.BeamRouter;
import beam.router.Modes;
import beam.router.RoutingModel;
import beam.sim.BeamServices;
import beam.sim.config.BeamConfig;
import beam.util.ConfigUtil;
import beam.utils.DateTimeUtil;
import com.conveyal.r5.api.ProfileResponse;
import com.conveyal.r5.api.util.*;
import com.conveyal.r5.profile.PathWithTimes;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.profile.RaptorState;
import com.conveyal.r5.transit.TransitLayer;
import com.conveyal.r5.transit.TransportNetwork;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.geotools.geometry.jts.CurvedGeometryFactory;
import org.jaitools.jts.CoordinateSequence2D;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Ahmar Nadeem
 */
@Ignore
public class R5RoutingWorkerTest {

    /*********************
     START - TEST DATA
     ********************/
    private static final double FROM_LONGITUDE = 40.689823;//X
    private static final double FROM_LATITUDE = -73.832639;//Y

    private static final double TO_LONGITUDE = 40.672770;//X
    private static final double TO_LATITUDE = -73.895639;//Y

    private static List<TransitJourneyID> transitJourneyIds;

    private static final String ZONE_ID = "-07:00";
    private static final String TIME = "T00:00:00";
    private static final String BASE_DATE = "2016-10-17";
    /****************
     END - TEST DATA
     ****************/

    private static BeamServices beamServices;
    private static TransportNetwork transportNetwork;
    private static ActorSystem system;
    private static ProfileResponse response;
    private static ProfileOption option;
    private static R5RoutingWorker actor;

    /**
     * @Description: This function is used to setup the data to be used by test functions at class startup time.
     */
    @BeforeClass
    public static void setup() {
        transitJourneyIds = buildTrasitJourneyIds();
        beamServices = mock(BeamServices.class);
        system = ActorSystem.create();
        response = mock(ProfileResponse.class);
        option = mock(ProfileOption.class);
        final Props props = R5RoutingWorker.props(beamServices);
        final TestActorRef<R5RoutingWorker> ref = TestActorRef.create(system, props, "calcRoute");
        actor = ref.underlyingActor();

        transportNetwork = mock(TransportNetwork.class);
        NetworkCoordinator.transportNetwork_$eq(transportNetwork);
    }

    /**
     * This function tests the funcionality of R5RoutingWorker.scala's buildRequest method.
     */
    @Test
    public void testBuildRequest() {

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);
        when(transportNetwork.getTimeZone()).thenReturn(ZoneId.systemDefault());

        Coord fromCoord = new Coord(FROM_LONGITUDE, FROM_LATITUDE);
//        Facility fromFacility = new FakeFacility(fromCoord);

        Coord toCoord = new Coord(TO_LONGITUDE, TO_LATITUDE);
//        Facility toFacility = new FakeFacility(toCoord);
        RoutingModel.BeamTime departureTime = new RoutingModel.WindowTime(DateTimeUtil.calculateTimeFromBase(null), 30);

        VectorBuilder<Modes.BeamMode> modeBuilder = new VectorBuilder<>();
        modeBuilder.$plus$eq((Modes.BeamMode) Modes.BeamMode$.MODULE$.withValue("walk"));
        Vector<Modes.BeamMode> modes = modeBuilder.result();

        BeamRouter.RoutingRequestTripInfo info = new BeamRouter.RoutingRequestTripInfo(fromCoord, toCoord, departureTime, modes, null, null);
        ProfileRequest request = null;//actor.buildRequest(info);
        /*assertNotNull(request);
        assertFalse(request.isProfile());
        assertEquals(4, request.bikeTrafficStress);
        assertEquals(4.0, request.bikeSpeed, 0.0);
        assertEquals(FROM_LATITUDE, request.fromLat, 0.0);
        assertEquals(FROM_LONGITUDE, request.fromLon, 0.0);
        assertEquals(TO_LATITUDE, request.toLat, 0.0);
        assertEquals(TO_LONGITUDE, request.toLon, 0.0);
        assertEquals(1.3, request.walkSpeed, 0.3);
        assertEquals(11.0, request.carSpeed, 0.0);
        assertEquals(60, request.streetTime);
        assertEquals(30, request.maxWalkTime);
        assertEquals(30, request.maxBikeTime);
        assertEquals(30, request.maxCarTime);
        assertEquals(5, request.minBikeTime);
        assertEquals(5, request.minCarTime);
        assertEquals(DateTimeUtil.convertStringToLocalDate(BASE_DATE), request.date);
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
        assertEquals(220, request.monteCarloDraws);*/
    }

    /**
     * This functions tests the behavior of build Response method of the class under test
     */
    @Ignore//TODO: RoutingModel.scala having infinite loop on BeamLeg. So ignoring it for now
    @Test
    public void testBuildResponse() {

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);

        ProfileResponse plan = new ProfileResponse();
        List<ProfileOption> options = new ArrayList<ProfileOption>();
        ProfileOption option = new ProfileOption();
        option.addItineraries(transitJourneyIds, ZoneId.systemDefault());
        option.itinerary = buildItineraries();
        option.access = buildAccess();
        option.transit = buildTransits();
        option.egress = buildEgressList();
        option.summary = "summary";
        option.fares = buildFares();
        options.add(option);
        response.options = options;

        when(response.getOptions()).thenReturn(options);
        R5RoutingWorker.TripFareTuple actualResponse = actor.buildResponse(response, true);
        assertNotNull(actualResponse);
    }

    /**
     * helper function to create a set of fares
     *
     * @return
     */
    private Set<Fare> buildFares() {
        Set<Fare> fareSet = new HashSet<Fare>();
        Fare fare = new Fare(150);
        fareSet.add(fare);
        return fareSet;
    }

    /**
     * helper function to buid transits list for mocking
     *
     * @return
     */
    private List<TransitSegment> buildTransits() {
        List<TransitSegment> transits = new ArrayList<>();
        TransitLayer tansitLayer = new TransitLayer();
        RaptorState state = new RaptorState(2, 5);
        int stop = 1;
        TransportNetwork network = new TransportNetwork();
        ProfileRequest req = new ProfileRequest();
        int[] boardStops = {1, 2, 3};
        int[] alightStops = {1, 2, 3};
        Stats[] waitStats = {new Stats()};
        Stats[] rideStats = {new Stats()};

        TIntIntMap accessTimes = new TIntIntHashMap(boardStops, alightStops);
        TIntIntMap egressTimes = new TIntIntHashMap(boardStops, alightStops);
//        PathWithTimes currentTransitPath = new PathWithTimes(state, stop, network, req, accessTimes, egressTimes);

        PathWithTimes currentTransitPath = mock(PathWithTimes.class);


        currentTransitPath.boardStops = boardStops;
        currentTransitPath.alightStops = alightStops;
        currentTransitPath.waitStats = waitStats;
        currentTransitPath.rideStats = rideStats;
        int pathIndex = 0;
        ZonedDateTime fromTimeDateZD = ZonedDateTime.now();
//        TransitSegment transit = new TransitSegment(tansitLayer, currentTransitPath, pathIndex, fromTimeDateZD, transitJourneyIds);
        TransitSegment transit = mock(TransitSegment.class);

        List<ZonedDateTime> zonedDateTimeList =  new ArrayList<>();
        zonedDateTimeList.add(ZonedDateTime.now());
        zonedDateTimeList.add(ZonedDateTime.now());
        zonedDateTimeList.add(ZonedDateTime.now());
        SegmentPattern segmentPattern = mock(SegmentPattern.class);
        segmentPattern.patternId = "1";
        segmentPattern.fromDepartureTime = zonedDateTimeList;
        List<SegmentPattern> segmentPatterns = new ArrayList<>();
        segmentPatterns.add(segmentPattern);
        segmentPatterns.add(segmentPattern);
        transit.segmentPatterns = segmentPatterns;
        transits.add(transit);
        return transits;
    }

    /**
     * helper function to build egress list for mocking
     *
     * @return
     */
    private List<StreetSegment> buildEgressList() {
        List<StreetSegment> egressList = new ArrayList<StreetSegment>();
        StreetSegment egress = new StreetSegment();
        egress.mode = LegMode.WALK;
        egressList.add(egress);
        return egressList;
    }

    /**
     * helper function to build access list for mocking
     *
     * @return
     */
    private List<StreetSegment> buildAccess() {

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
        int accessIndex = 0;
        GeometryFactory factory = new CurvedGeometryFactory(accessIndex);
        LineString geom = new LineString(points, factory);
        streetEdge.geometry = geom;
        streetEdges.add(streetEdge);
        accessSegment.streetEdges = streetEdges;
        accessList.add(accessSegment);
        return accessList;
    }

    /**
     * helper function to build itineraries list for mocking
     *
     * @return
     */
    private List<Itinerary> buildItineraries() {

        List<Itinerary> itineraries = new ArrayList<Itinerary>();
        Itinerary itinerary = new Itinerary();
        int accessIndex = 0;
        int egressIndex = 0;
        PointToPointConnection p2pConnection = new PointToPointConnection(accessIndex, egressIndex, transitJourneyIds);
        itinerary.connection = p2pConnection;
        itinerary.startTime = ZonedDateTime.now();
        itinerary.endTime = ZonedDateTime.now().plusHours(2);
        itineraries.add(itinerary);
        return itineraries;
    }

    /**
     * helper function to mock transit journey ids.
     *
     * @return
     */
    private static List<TransitJourneyID> buildTrasitJourneyIds() {
        transitJourneyIds = new ArrayList<TransitJourneyID>();
        TransitJourneyID transitJourneyId = new TransitJourneyID(1, 2);
        transitJourneyIds.add(transitJourneyId);
        return transitJourneyIds;
    }


    /**
     * Test the calcRoute function in the R5RoutingWorker class.
     */
    @Ignore
    @Test
    public void testCalculateRoute() {

        BeamConfig beamConfig = ConfigUtil.buildDefaultConfig();
        beamServices.beamConfig_$eq(beamConfig);

        when(beamServices.beamConfig()).thenReturn(beamConfig);
        when(transportNetwork.getTimeZone()).thenReturn(ZoneId.systemDefault());

        Coord fromCoord = new Coord(FROM_LONGITUDE, FROM_LATITUDE);
//        Facility fromFacility = new FakeFacility(fromCoord);

        Coord toCoord = new Coord(TO_LONGITUDE, TO_LATITUDE);
//        Facility toFacility = new FakeFacility(toCoord);

        RoutingModel.BeamTime departureTime = new RoutingModel.WindowTime(DateTimeUtil.calculateTimeFromBase(null), 30);

        VectorBuilder<Modes.BeamMode> modeBuilder = new VectorBuilder<>();
        modeBuilder.$plus$eq((Modes.BeamMode) Modes.BeamMode$.MODULE$.withValue("walk"));
        Vector<Modes.BeamMode> modes = modeBuilder.result();

        Person person = null;

        BeamRouter.RoutingResponse response = actor.calcRoute(BeamRouter.nextId(), new BeamRouter.RoutingRequestTripInfo(fromCoord, toCoord, departureTime, modes, null, null), person);
        assertTrue(response != null);
    }
}
