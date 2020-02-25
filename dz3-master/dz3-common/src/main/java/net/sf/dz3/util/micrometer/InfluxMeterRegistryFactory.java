package net.sf.dz3.util.micrometer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

/**
 * Simple wrapper to get {@link InfluxMeterRegistry} without the hassle of
 * writing a complex Spring configuration. May need to be discarded or improved
 * later.
 */
public class InfluxMeterRegistryFactory {

    private final MeterRegistry mr;
    
    public InfluxMeterRegistryFactory() {

        InfluxConfig cf = new InfluxConfig() {
            @Override
            public String db() {
                return "dz";
            }

            @Override
            public String get(String key) {
                // Accept the defaults
                return null;
            }
        };
        
        mr = new InfluxMeterRegistry(cf, Clock.SYSTEM);

    }

    public MeterRegistry getRegistry() {
        return mr;
    }
}
