package beam.util;

import beam.sim.config.BeamConfig;
import org.junit.Test;

public class ConfigUtilTest {
    @Test
    public void testBuildDefaultConfig(){
        BeamConfig cfg = beam.util.ConfigUtil.buildDefaultConfig();
        assert (cfg.beam().routing().baseDate().equalsIgnoreCase("2016-10-17T00:00:00-07:00"));
    }

    @Test
    public void testBuildDefaultRouting(){
        assert (beam.util.ConfigUtil.buildDefaultRouting().baseDate().equalsIgnoreCase("2016-10-17T00:00:00-07:00"));
    }

    @Test
    public void testBuildRoutingWithBaseDate(){
        assert (beam.util.ConfigUtil.buildRoutingWithBaseDate("2016-10-17T00:00:00-07:00").baseDate().equalsIgnoreCase("2016-10-17T00:00:00-07:00"));
    }
}
