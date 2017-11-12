package beam.agentsim.events.handling;

import beam.agentsim.events.LoggerLevels;
import beam.agentsim.events.ModeChoiceEvent;
import beam.agentsim.events.PathTraversalEvent;
import beam.sim.BeamServices;
import beam.utils.DebugLib;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.*;

import static beam.agentsim.events.LoggerLevels.OFF;

/**
 * BEAM
 */

public class BeamEventsLogger implements IterationStartsListener, IterationEndsListener {
    private final EventsManager eventsManager;
    private ArrayList<BeamEventsWriterBase> writers = new ArrayList<>();

    private HashMap<Class<?>, LoggerLevels> levels = new HashMap<>();
    private LoggerLevels defaultLevel;

    private HashSet<Class<?>> allLoggableEvents = new HashSet<>(), eventsToLog = new HashSet<>();
    private BeamServices beamServices;
    private ArrayList<BeamEventsFileFormats> eventsFileFormatsArray = new ArrayList<>();

    // create multimap to store key and values
    Multimap<Class, String> eventFieldsToDropWhenShort = ArrayListMultimap.create();
    private Multimap<Class, String> eventFieldsToAddWhenVerbose = ArrayListMultimap.create();

    @javax.inject.Inject
    BeamEventsLogger(BeamServices beamServices, EventsManager eventsManager) {

        this.beamServices = beamServices;
        this.eventsManager = eventsManager;
        setEventsFileFormats();

        // Registry of BEAM events that can be logged by BeamEventLogger
        allLoggableEvents.add(PathTraversalEvent.class);
        allLoggableEvents.add(ModeChoiceEvent.class);

        // Registry of MATSim events that can be logged by BeamEventLogger
        allLoggableEvents.add(ActivityEndEvent.class);
        allLoggableEvents.add(PersonDepartureEvent.class);
        allLoggableEvents.add(PersonEntersVehicleEvent.class);
        allLoggableEvents.add(VehicleEntersTrafficEvent.class);
        allLoggableEvents.add(LinkLeaveEvent.class);
        allLoggableEvents.add(LinkEnterEvent.class);
        allLoggableEvents.add(VehicleLeavesTrafficEvent.class);
        allLoggableEvents.add(PersonLeavesVehicleEvent.class);
        allLoggableEvents.add(PersonArrivalEvent.class);
        allLoggableEvents.add(ActivityStartEvent.class);

        //filter according loggerLevel
        if (this.beamServices.beamConfig().beam().outputs().defaultLoggingLevel().equals("")) {
            defaultLevel = OFF;
        } else {
            defaultLevel = LoggerLevels.valueOf(this.beamServices.beamConfig().beam().outputs().defaultLoggingLevel());
            eventsToLog.addAll(getAllLoggableEvents());
        }
        overrideDefaultLoggerSetup();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        boolean writeThisIteration = (this.beamServices.beamConfig().beam().outputs().writeEventsInterval() > 0) && (event.getIteration() % this.beamServices.beamConfig().beam().outputs().writeEventsInterval() == 0);
        if (writeThisIteration) {
            this.beamServices.matsimServices().getControlerIO().createIterationDirectory(event.getIteration());
            String eventsFileBasePath = this.beamServices.matsimServices().getControlerIO().getIterationFilename(event.getIteration(), "events");

            for (BeamEventsFileFormats fmt : this.eventsFileFormatsArray) {
                BeamEventsWriterBase newWriter = null;
                if (this.beamServices.beamConfig().beam().outputs().explodeEventsIntoFiles()) {
                    for (Class<?> eventTypeToLog : getAllEventsToLog()) {
                        newWriter = createEventWriterForClassAndFormat(eventsFileBasePath, eventTypeToLog, fmt);
                        writers.add(newWriter);
                        eventsManager.addHandler(newWriter);
                    }
                } else {
                    newWriter = createEventWriterForClassAndFormat(eventsFileBasePath, null, fmt);
                    writers.add(newWriter);
                    eventsManager.addHandler(newWriter);
                }
            }
        }
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        for (BeamEventsWriterBase writer : this.writers) {
            writer.closeFile();
            this.eventsManager.removeHandler(writer);
        }
        this.writers.clear();
    }

    private BeamEventsWriterBase createEventWriterForClassAndFormat(String eventsFilePathBase, Class<?> theClass, BeamEventsFileFormats fmt) {
        if (fmt == BeamEventsFileFormats.xml || fmt == BeamEventsFileFormats.xmlgz) {
            String path = eventsFilePathBase + ((fmt == BeamEventsFileFormats.xml) ? ".xml" : ".xml.gz");
            return new BeamEventsWriterXML(path, this, this.beamServices, theClass);
        } else if (fmt == BeamEventsFileFormats.csv || fmt == BeamEventsFileFormats.csvgz) {
            String path = eventsFilePathBase + ((fmt == BeamEventsFileFormats.csv) ? ".csv" : ".csv.gz");
            return new BeamEventsWriterCSV(path, this, this.beamServices, theClass);
        }
        return null;
    }

    private void setLoggingLevel(Class<?> eventType, LoggerLevels level) {
        levels.put(eventType, level);
    }

    //Logging control code changed return type from int to String
    LoggerLevels getLoggingLevel(Event event) {
        return getLoggingLevel(event.getClass());
    }

    LoggerLevels getLoggingLevel(Class clazz) {
        return levels.getOrDefault(clazz, defaultLevel);
    }

    boolean shouldLogThisEventType(Class<? extends Event> aClass) {
        //TODO in future this is where fine tuning logging based on level number could occur (e.g. info versus debug)
        return eventsToLog.contains(aClass);
    }

    HashSet<Class<?>> getAllEventsToLog() {
        return eventsToLog;
    }

    private HashSet<Class<?>> getAllLoggableEvents() {
        return allLoggableEvents;
    }

    private void setEventsFileFormats() {
        BeamEventsFileFormats fmt = null;
        String eventsFileFormats = this.beamServices.beamConfig().beam().outputs().eventsFileOutputFormats();
        this.eventsFileFormatsArray.clear();
        for (String format : eventsFileFormats.split(",")) {
            if (format.equalsIgnoreCase("xml")) {
                fmt = BeamEventsFileFormats.xml;
            } else if (format.equalsIgnoreCase("xml.gz")) {
                fmt = BeamEventsFileFormats.xmlgz;
            } else if (format.equalsIgnoreCase("csv")) {
                fmt = BeamEventsFileFormats.csv;
            } else if (format.equalsIgnoreCase("csv.gz")) {
                fmt = BeamEventsFileFormats.csvgz;
            }
            this.eventsFileFormatsArray.add(fmt);
        }
    }

    public Map<String, String> getAttributes(Event event) {
        Map<String, String> attributes = event.getAttributes();
        //Remove attribute from each event class for SHORT logger level
        List<String> eventFields = null;
        if (getLoggingLevel(event) == LoggerLevels.SHORT && eventFieldsToDropWhenShort.containsKey(event.getClass())) {
            eventFields = (List) eventFieldsToDropWhenShort.get(event.getClass());
            // iterate through the key set
            for (String key : eventFields) {
                attributes.remove(key);
            }
        }
        //Add attribute from each event class for VERBOSE logger level
        else if (getLoggingLevel(event) == LoggerLevels.VERBOSE && eventFieldsToAddWhenVerbose.containsKey(event.getClass())) {
            eventFields = (List) eventFieldsToAddWhenVerbose.get(event.getClass());
            // iterate through the key set
//            for (String key : eventFields) {
//                attributes.putAll(event.getVer);
//            }
        }
        return attributes;
    }

    private void overrideDefaultLoggerSetup() {
        Class<?> theClass = null;

        for (String classAndLevel : beamServices.beamConfig().beam().outputs().overrideLoggingLevels().split(",")) {
            String[] splitClassLevel = classAndLevel.split(":");
            String classString = splitClassLevel[0].trim();
            String levelString = splitClassLevel[1].trim();
            LoggerLevels theLevel = LoggerLevels.valueOf(levelString);
            try {
                theClass = Class.forName(classString);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                DebugLib.stopSystemAndReportInconsistency("Logging class name '" + theClass.getCanonicalName() + "' is not a valid class, use fully qualified class names (e.g. .");
            }
            setLoggingLevel(theClass, theLevel);
            if (theLevel != OFF) {
                eventsToLog.add(theClass);
            } else {
                eventsToLog.remove(theClass);
            }
        }
    }

}
