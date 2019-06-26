package net.sf.dz3.view.influxdb.v1;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.ThreadContext;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;

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
public class InfluxDbLogger<E extends Number> extends AbstractLogger<E> {

    private final String dbName = "dz";
    private final String instance;
    private final String dbURL;
    private final String username;
    private final String password;

    private InfluxDB db;

    /**
     * Create an unauthenticated instance.
     * Using this constructor is discouraged, but convenient.
     *
     * @param producers Data producers.
     * @param instance Unique identifier for an instance this logger represents. This value would usually correspond
     * to a host a DZ instance runs on.
     * @param dbURL InfluxDB URL to connect to.
     */
    public InfluxDbLogger(
            Set<DataSource<E>> producers,
            String instance,
            String dbURL) {
        this(producers, instance, dbURL, null, null);
    }

    /**
     * Create an instance.
     *
     * @param producers Data producers.
     * @param instance Unique identifier for an instance this logger represents. This value would usually correspond
     * to a host a DZ instance runs on.
     * @param dbURL InfluxDB URL to connect to.
     * @param username InfluxDB username. Use {@code null} for unauthenticated access}.
     * @param password InfluxDB password. Use {@code null} for unauthenticated access}.
     */
    public InfluxDbLogger(
            Set<DataSource<E>> producers,
            String instance,
            String dbURL,
            String username,
            String password) {
        super(producers);

        this.instance = instance;
        this.dbURL = dbURL;
        this.username = username;
        this.password = password;
    }

    @Override
    protected void consume(String signature, DataSample<E> value) {
        ThreadContext.push("consume");

        try {

            db.write(Point.measurement(value.sourceName)
                    .time(value.timestamp, TimeUnit.MILLISECONDS)
                    .addField("signature", value.signature)
                    .addField("instance", instance)
                    .addField("sample", value.sample)
                    .build());

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void createChannel(String name, String signature, long timestamp) throws IOException {
        // Nothing to do here, channels are tags here
    }

    private String doubleQuote(String source) {
        return "\"" + source + "\"";
    }

    @Override
    protected void startup() throws Throwable {

        ThreadContext.push("startup");

        try {

            if (username == null || "".equals(username) || password == null || "".equals(password)) {
                logger.warn("one of (username, password) is null or missing, connecting unauthenticated - THIS IS A BAD IDEA");
                logger.warn("see https://docs.influxdata.com/influxdb/v1.7/administration/authentication_and_authorization/");
                logger.warn("(username, password) = (" + username + ", " + password + ")");

                db = InfluxDBFactory.connect(dbURL);

            } else {
                db = InfluxDBFactory.connect(dbURL, username, password);
            }

            db.query(new Query("CREATE DATABASE " + dbName));
            db.setDatabase(dbName);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void shutdown() throws Throwable {

        ThreadContext.push("shutdown");

        try {

            db.close();

        } finally {
            ThreadContext.pop();
        }
    }
}
