package beam.util;

import beam.sim.config.BeamConfig;
import org.junit.Test;

public class ConfigUtilTest {

    public static final String BASE_DATE = "2016-10-17T00:00:00-07:00";

    @Test
    public void testBuildDefaultConfig(){
        BeamConfig cfg = beam.util.ConfigUtil.buildDefaultConfig();
        assert (BASE_DATE.equalsIgnoreCase(cfg.beam().routing().baseDate()));
    }

    @Test
    public void testBuildDefaultRouting(){
        assert (BASE_DATE.equalsIgnoreCase(beam.util.ConfigUtil.buildDefaultRouting().baseDate()));
    }

    @Test
    public void testBuildRoutingWithBaseDate(){
        assert (BASE_DATE.equalsIgnoreCase(beam.util.ConfigUtil.buildRoutingWithBaseDate(BASE_DATE).baseDate()));
    }

    @Test
    public void testBuildRoutingWithBaseDate_Failuer(){
        assert (!BASE_DATE.equalsIgnoreCase(beam.util.ConfigUtil.buildRoutingWithBaseDate(null).baseDate()));
    }
}
