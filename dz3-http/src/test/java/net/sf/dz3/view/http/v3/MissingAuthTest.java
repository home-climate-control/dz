package net.sf.dz3.view.http.v3;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.view.http.common.AbstractExchanger;
import net.sf.dz3.view.http.common.BufferedExchanger;
import net.sf.dz3.view.http.common.ContextChecker;

/**
 * Test for {@link AbstractExchanger} being instantiated without username and password
 * (required for {@link HttpConnector}.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class MissingAuthTest {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Test target.
     *
     * Must return HTTP 200 OK when called with GET.
     */
    private final String TARGET_URL = "test.httpconnector3.url";

    @Test
    public void testNoAuth() throws IOException, InterruptedException {

        ThreadContext.push("testNoAuth");
        Marker m = new Marker("testNoAuth");

        try {

            if (!ContextChecker.runNow(logger)) {
                return;
            }

            Map<String, String> env = ContextChecker.check(new String[] {TARGET_URL}, true);

            String targetUrl = env.get(TARGET_URL);
            BlockingQueue<String> upstreamQueue = new LinkedBlockingQueue<String>();
            CountDownLatch completionGate = new CountDownLatch(1);
            DummyExchanger exchanger = new DummyExchanger(new URL(targetUrl), upstreamQueue, completionGate);

            exchanger.setMaxBufferAgeMillis(1000);
            exchanger.start().waitFor();

            // This should push the exchanger over the edge
            upstreamQueue.put("oops");

            logger.debug("waiting for the exchange to happen");

            completionGate.await();

            m.checkpoint("exchange completed");

            exchanger.stop().waitFor();
            logger.debug("stopped the exchanger");

            assertEquals("wrong status", 200, exchanger.status.get());

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    private class DummyExchanger extends BufferedExchanger<String> {

        private final CountDownLatch completionGate;
        public final AtomicInteger status = new AtomicInteger(0);

        public DummyExchanger(URL serverContextRoot, BlockingQueue<String> upstreamQueue, CountDownLatch completionGate) {
            super(serverContextRoot, null, null, upstreamQueue);

            this.completionGate = completionGate;
        }

        @Override
        protected void exchange(List<String> buffer) {

            ThreadContext.push("exchange");

            try {

                logger.debug("buffer: " + buffer);

                HttpGet get = new HttpGet(serverContextRoot.toString());
                HttpResponse rsp = httpClient.execute(get, context);
                int rc = rsp.getStatusLine().getStatusCode();

                if (rc == 200) {

                    logger.info("HTTP rc=" + rc + ", text follows:");
                    logger.debug(EntityUtils.toString(rsp.getEntity()));

                } else {

                    logger.error("HTTP rc=" + rc + ", text follows:");
                    logger.error(EntityUtils.toString(rsp.getEntity()));
                }

                status.set(rc);
                completionGate.countDown();

            } catch (IOException ex) {

                logger.error("exchange failed", ex);

            } finally {
                ThreadContext.pop();
            }
        }
    }
}
