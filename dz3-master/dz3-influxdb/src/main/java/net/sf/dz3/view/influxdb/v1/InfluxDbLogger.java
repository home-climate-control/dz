package net.sf.dz3.view.influxdb.v1;

import java.io.IOException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
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
    private final Queue<DataSample<E>> queue = new LinkedBlockingQueue<>();
    private final int QUEUE_MAX = 1024;

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

            if (queue.size() < QUEUE_MAX) {

                // The cost of doing this all this time is negligible

                queue.add(value);

            } else {
                logger.error("QUEUE_MAX=" + QUEUE_MAX + " exceeded, skipping sample: " + value);
            }

            synchronized (this) {

                // This happens at startup, when the connection is not yet established,
                // but the instance is ready to accept samples

                if (db == null) {
                    logger.warn("no connection yet, " + queue.size() + " sample[s] deferred");
                    return;
                }
            }

            while (!queue.isEmpty()) {

                try {

                    DataSample<E> sample = queue.peek();

                    db.write(Point.measurement(sample.sourceName)
                            .time(sample.timestamp, TimeUnit.MILLISECONDS)
                            .addField("instance", instance)
                            .addField("signature", sample.signature)
                            .addField("sample", sample.sample)
                            .build());

                    queue.remove();

                } catch (Throwable t) {

                    // The item we couldn't write is still in the queue

                    logger.warn("can't write sample, deferring remaining " + queue.size() + " samples for now", t);
                    break;
                }
            }

            db.flush();

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void createChannel(String name, String signature, long timestamp) throws IOException {
        // Nothing to do here, channels are tags here
    }

    @Override
    protected void startup() throws Throwable {

        ThreadContext.push("startup");

        try {

            connect();

            db.enableBatch();
            db.query(new Query("CREATE DATABASE " + dbName));
            db.setDatabase(dbName);

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Connect to the remote in a non-blocking way.
     */
    private void connect() {

        InfluxDB db;

        // This section will not block synchronized calls

        if (username == null || "".equals(username) || password == null || "".equals(password)) {
            logger.warn("one of (username, password) is null or missing, connecting unauthenticated - THIS IS A BAD IDEA");
            logger.warn("see https://docs.influxdata.com/influxdb/v1.7/administration/authentication_and_authorization/");
            logger.warn("(username, password) = (" + username + ", " + password + ")");

            db = InfluxDBFactory.connect(dbURL);

        } else {
            db = InfluxDBFactory.connect(dbURL, username, password);
        }

        // This section is short and won't delay other synchronized calls much

        synchronized (this) {
            this.db = db;
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
