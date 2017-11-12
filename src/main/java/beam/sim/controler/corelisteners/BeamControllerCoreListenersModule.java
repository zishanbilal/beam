package beam.sim.controler.corelisteners;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;

public class BeamControllerCoreListenersModule extends AbstractModule {

    @Override
    public void install() {
        install(new beam.agentsim.events.handling.PlansScoringModule());
        bind(PlansReplanning.class).to(PlansReplanningImpl.class);
        bind(PlansDumping.class).to(PlansDumpingImpl.class);
        bind(DumpDataAtEnd.class).to(DumpDataAtEndImpl.class);
        bind(beam.agentsim.events.handling.EventsToActivities.class).asEagerSingleton();
    }
}

