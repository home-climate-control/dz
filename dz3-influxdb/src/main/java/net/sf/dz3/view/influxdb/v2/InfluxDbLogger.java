package net.sf.dz3.view.influxdb.v2;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.view.influxdb.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;

/**
 * Simple InfluxDB logger.
 *
 * Younger twin brother of {@link net.sf.dz3.view.influxdb.v1.InfluxDbLogger}, implemented with Reactive Streams.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class InfluxDbLogger implements Subscriber<DataSample<Number>> {

    private final Logger logger = LogManager.getLogger();
    private final Config config;
    private InfluxDB db;

    /**
     * Create an instance.
     *
     * @param instance Unique identifier for an instance this logger represents. This value would usually correspond
     * to a host a DZ instance runs on.
     * @param dbURL InfluxDB URL to connect to.
     * @param username InfluxDB username. Use {@code null} for unauthenticated access}.
     * @param password InfluxDB password. Use {@code null} for unauthenticated access}.
     */
    public InfluxDbLogger(
            String instance,
            String dbURL,
            String username,
            String password) {

        config = new Config(instance, dbURL, username, password);

        connect();
    }

    private void connect() {

        if (config.username == null || "".equals(config.username) || config.password == null || "".equals(config.password)) {
            logger.warn("one of (username, password) is null or missing, connecting unauthenticated - THIS IS A BAD IDEA");
            logger.warn("see https://docs.influxdata.com/influxdb/v1.7/administration/authentication_and_authorization/");
            logger.warn("(username, password) = ({}, {})", config.username, config.password);

            db = InfluxDBFactory.connect(config.dbURL);

        } else {
            db = InfluxDBFactory.connect(config.dbURL, config.username, config.password);
        }

        db.enableBatch();
        var dbName = "dz";
        db.query(new Query("CREATE DATABASE " + dbName));
        db.setDatabase(dbName);
    }

    @Override
    public void onSubscribe(Subscription s) {
        // No limit for now. Need to see if buffering is required.
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(DataSample<Number> sample) {

        Point.Builder b = Point.measurement("sensor")
                .time(sample.timestamp, TimeUnit.MILLISECONDS)
                .tag("instance", config.instance)
                .tag("source", sample.sourceName)
                .tag("signature", sample.signature);

        // These two are mutually exclusive

        if (sample.isError()) {
            b.addField("error", sample.error.toString());
        } else {
            b.addField("sample", sample.sample);
        }

        db.write(b.build());
    }

    @Override
    public void onError(Throwable t) {
        logger.error("onError", t);
    }

    @Override
    public void onComplete() {
        logger.warn("onComplete()");
        db.close();
    }
}
