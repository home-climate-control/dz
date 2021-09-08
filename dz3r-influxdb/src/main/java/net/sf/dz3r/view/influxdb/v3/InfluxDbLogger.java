package net.sf.dz3r.view.influxdb.v3;

import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.influxdb.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simple InfluxDB logger.
 *
 * Younger twin brother of {@code net.sf.dz3.view.influxdb.v1.InfluxDbLogger}, implemented with Reactive Streams.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class InfluxDbLogger implements Subscriber<Signal<Number, Map<String, String>>> {

    private final Logger logger = LogManager.getLogger();
    private final Config config;
    private InfluxDB db;

    /**
     * Create an instance with a default {@code "dz"} database name.
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

        this("dz", instance, dbURL, username, password);
    }

    /**
     * Create an instance.
     *
     * @param dbName Database name to use.
     * @param instance Unique identifier for an instance this logger represents. This value would usually correspond
     * to a host a DZ instance runs on.
     * @param dbURL InfluxDB URL to connect to.
     * @param username InfluxDB username. Use {@code null} for unauthenticated access}.
     * @param password InfluxDB password. Use {@code null} for unauthenticated access}.
     */
    public InfluxDbLogger(
            String dbName,
            String instance,
            String dbURL,
            String username,
            String password) {

        config = new Config(dbName, instance, dbURL, username, password);

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
        db.query(new Query("CREATE DATABASE \"" + config.dbName + "\""));
        db.setDatabase(config.dbName);
    }

    @Override
    public void onSubscribe(Subscription s) {
        // No limit for now. Need to see if buffering is required.
        s.request(Long.MAX_VALUE);
    }

    /**
     * Consume the signal and write it to the target database.
     *
     * @param signal Signal to write to InfluxDB. The value is the measurement, the payload is the tag set.
     */
    @Override
    public void onNext(Signal<Number, Map<String, String>> signal) {

        Point.Builder b = Point.measurement("sensor")
                .time(signal.timestamp.toEpochMilli(), TimeUnit.MILLISECONDS)
                .tag("instance", config.instance);

        for (var kv : signal.payload.entrySet()) {
            b.tag(kv.getKey(), kv.getValue());
        }

        if (signal.getValue() != null) {
            b.addField("sample", signal.getValue());
        }

        if (signal.error != null) {
            b.addField("error", signal.error.toString());
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
