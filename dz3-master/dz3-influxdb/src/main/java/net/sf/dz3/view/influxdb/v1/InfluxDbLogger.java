package net.sf.dz3.view.influxdb.v1;

import java.io.IOException;
import java.util.Set;

import net.sf.jukebox.datastream.logger.impl.AbstractLogger;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSource;

/**
 * Simple InfluxDB logger.
 *
 * This class serves the same purpose that {@link InfluxDbConnector} finally
 * will, but the functionality is straightforward and limited.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public class InfluxDbLogger extends AbstractLogger<Number> {

    public InfluxDbLogger(Set<DataSource<Number>> producers) {
        super(producers);
    }

    @Override
    protected void consume(String signature, DataSample<Number> value) {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void createChannel(String arg0, String arg1, long arg2) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void startup() throws Throwable {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void shutdown() throws Throwable {
        throw new IllegalStateException("Not Implemented");
    }
}
