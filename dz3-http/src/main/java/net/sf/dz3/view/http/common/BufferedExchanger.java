package net.sf.dz3.view.http.common;

import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import org.apache.logging.log4j.ThreadContext;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Keeps sending data that appears in {@code HttpConnector#upstreamQueue} to the server
 * once in a while, and accepting whatever they have to say.
 *
 * @param <T> Data type to send out to the server.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class BufferedExchanger<T> extends AbstractExchanger<T> {

    /**
     * Maximum age of the buffer before it gets sent, regardless of whether it is empty or not.
     *
     * Careful, setting it too low will pound the server with empty packets. This will
     * improve response latency, but create a lot of overhead and may exhaust the quota very quickly.
     */
    private long maxBufferAgeMillis = 10000L;

    protected BufferedExchanger(URL serverContextRoot, String username, String password, BlockingQueue<T> upstreamQueue) {
        super(serverContextRoot, username, password, upstreamQueue);
    }

    @JmxAttribute(description="Maximum age of the buffer before it gets sent, in milliseconds")
    public long getMaxBufferAgeMillis() {
        return maxBufferAgeMillis;
    }

    /**
     * Set the maximum buffer age.
     *
     * @param maxBufferAgeMillis Maximum buffer age, in milliseconds.
     */
    public void setMaxBufferAgeMillis(long maxBufferAgeMillis) {

        if (maxBufferAgeMillis < 1000) {
            throw new IllegalArgumentException("Unreasonably short buffer age (" + maxBufferAgeMillis + "), min is 1000ms");
        }

        this.maxBufferAgeMillis = maxBufferAgeMillis;
    }

    /**
     * Keep sending data that appears in {@code HttpConnector#upstreamQueue} to the server
     * right away, and accepting whatever they have to say.
     */
    @Override
    protected void execute() throws Throwable {

        ThreadContext.push("execute");

        try {

            while (isEnabled()) {

                try {

                    synchronized (this) {
                        wait(maxBufferAgeMillis);
                    }

                    var buffer = new LinkedList<T>();

                    while (!upstreamQueue.isEmpty()) {
                        buffer.add(upstreamQueue.remove());
                    }

                    exchange(buffer);

                } catch (Throwable t) { // NOSONAR Consequences have been considered

                    // Can't afford to bail out, this may be a transient condition
                    logger.error("Unexpected exception", t);
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    protected abstract void exchange(List<T> buffer);
}
