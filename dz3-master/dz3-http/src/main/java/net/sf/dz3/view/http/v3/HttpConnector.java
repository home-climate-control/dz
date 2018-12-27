package net.sf.dz3.view.http.v3;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.ThreadContext;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.instrumentation.Marker;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.dz3.view.http.common.BufferedExchanger;
import net.sf.dz3.view.http.common.QueueFeeder;
import net.sf.dz3.view.http.v2.JsonRenderer;
import net.sf.dz3.view.http.v2.ThermostatFactory;
import net.sf.dz3.view.http.v2.ZoneCommand;
import net.sf.dz3.view.http.v2.ZoneSnapshot;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * HTTP client side interface.
 * 
 * This object is supposed to be instantiated via Spring configuration file, with objects
 * that are supposed to be rendered and/or controlled being present in a set passed to the constructor.
 * 
 * See {@code net.sf.dz3.view.swing.Console} for more information.
 * 
 * {@code init-method="start"} attribute must be used in Spring bean definition, otherwise
 * the connector will not work.
 * 
 * The difference between this and {@link net.sf.dz3.view.http.v2.HttpConnector previous implementation}
 * is the authentication and core/remote matching method. Previous versions used HTTP Basic authentication
 * which is no longer supported by GAE ({@link https://developers.google.com/api-client-library/java/google-api-java-client/client-login}).
 *
 * OAuth 2.0 support seems to only enable "access to Google APIs" which DZ doesn't care about, efficiency
 * being the reason. Hence, DZ will be matching the message digest of the email used to authenticate
 * both on HCC Core and HCC Remote side on the proxy in this protocol version.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class HttpConnector extends Connector<JsonRenderer>{

    private final BlockingQueue<ZoneSnapshot> upstreamQueue = new LinkedBlockingQueue<ZoneSnapshot>();
    private final URL serverContextRoot;
    private final BufferedExchanger<ZoneSnapshot> exchanger;
    private final Gson gson = new Gson();

    /**
     * Create an instance and fill it up with objects to render.
     * 
     * @param initSet Objects to display.
     */
    public HttpConnector(URL serverContextRoot, Set<Object> initSet) {

        super(initSet);
        
        this.serverContextRoot = serverContextRoot;
        
        exchanger = new ZoneSnapshotExchanger(serverContextRoot, upstreamQueue);
        
        Scheduler scheduler = null;
        
        for (Iterator<Object> i = initSet.iterator(); i.hasNext(); ) {
            
            Object maybeScheduler = i.next();
            
            if (maybeScheduler instanceof Scheduler) {
                
                logger.debug("Found scheduler: " + maybeScheduler);
                scheduler = (Scheduler) maybeScheduler;
            }
        }
        
        logger.info("Using scheduler: " + scheduler);
        
        if (scheduler == null) {

            logger.error("No scheduler found, no schedule deviations will be reported to remote controls");
        }

        register(ThermostatModel.class, new ThermostatFactory(scheduler));
    }

    /**
     * Create an instance and fill it up with objects to render,
     * using custom factory set.
     * 
     * @param initSet Objects to display.
     * @param factorySet Set of {@link ConnectorFactory} objects to use for component creation.
     */
    public HttpConnector(URL serverBase, Set<Object> initSet, Set<ConnectorFactory<JsonRenderer>> factorySet) {
        
        super(initSet, factorySet);
        
        this.serverContextRoot = serverBase;

        exchanger = new ZoneSnapshotExchanger(serverContextRoot, upstreamQueue);
    }

    @Override
    protected void activate2() {

        exchanger.start();
    }

    @Override
    protected Map<String, Object> createContext() {

        Map<String, Object> context = new TreeMap<String, Object>();
        
        context.put(QueueFeeder.QUEUE_KEY, upstreamQueue);
        return context;
    }

    @Override
    protected void deactivate2() {

        exchanger.stop();
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        int port = serverContextRoot.getPort();

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                serverContextRoot.getProtocol()
                + " " + serverContextRoot.getHost()
                + (port == -1 ? "" : " port " + port)
                + " " + serverContextRoot.getPath(),
                "HTTP Client v2");
    }
    
    @JmxAttribute(description = "Upstream queue size")
    public final int getQueueSize() {
        
        return upstreamQueue.size();
    }

    @JmxAttribute(description="Maximum age of the buffer before it gets sent, in milliseconds")
    public long getMaxBufferAgeMillis() {
        
        return exchanger.getMaxBufferAgeMillis();
    }

    /**
     * Set the maximum buffer age.
     * 
     * @param maxBufferAgeMillis Maximum buffer age, in milliseconds.
     */
    public void setMaxBufferAgeMillis(long maxBufferAgeMillis) {
        
        exchanger.setMaxBufferAgeMillis(maxBufferAgeMillis);
    }

    private class ZoneSnapshotExchanger extends BufferedExchanger<ZoneSnapshot> {

        public ZoneSnapshotExchanger(URL serverContextRoot, BlockingQueue<ZoneSnapshot> upstreamQueue) {

            super(serverContextRoot, null, null, upstreamQueue);
        }

        @Override
        protected final void exchange(List<ZoneSnapshot> buffer) {
            
            ThreadContext.push("exchange");
            Marker m = new Marker("exchange");
            
            try {

                logger.debug("Sending: " + buffer);
                
                String encoded = gson.toJson(buffer);

                logger.debug("JSON: " + encoded);

                URL targetUrl = serverContextRoot;
                URIBuilder builder = new URIBuilder(targetUrl.toString());

                HttpPost post = new HttpPost(builder.toString());

                post.setHeader("HCC-Identity", getIdentity());
                post.setEntity(new StringEntity(encoded));
                
                try {

                    HttpResponse rsp = httpClient.execute(post);
                    int rc = rsp.getStatusLine().getStatusCode();

                    if (rc != 200) {

                        logger.error("HTTP rc=" + rc + ", text follows:");
                        logger.error(EntityUtils.toString(rsp.getEntity()));
                        
                        throw new IOException("Request to " + targetUrl + " failed with HTTP code " + rc);
                    }
                    
                    processResponse(EntityUtils.toString(rsp.getEntity()));
                    
                } finally {
                    post.releaseConnection();
                }
                
            } catch (Throwable t) {
                
                // VT: FIXME: For now, this is not a recoverable problem, the snapshot is
                // irretrievably lost. Need to see if this matters at all
                
                logger.error("Buffer exchange failed", t);
            
            } finally {

                m.close();
                ThreadContext.pop();
            }
        }

        /**
         * @return Client identity to be sent to the proxy.
         */
        private String getIdentity() throws IOException, InterruptedException {

            ThreadContext.push("getIdentity");

            try {

                // VT: NOTE: This needs to be done every time because since last call,
                // either the access token expired (need to refresh),
                // or it could've been revoked (need to reacquire permissions

                OAuth2DeviceIdentityProvider provider = new OAuth2DeviceIdentityProvider();

                File base = getSecretsDir();
                String identity = provider.getIdentity(
                        new File(base, "client-id"),
                        new File(base, "client-secret"),
                        new File(base, "token"),
                        "HttpConnector");

                logger.debug("identity: " + identity);

                return identity;

            } finally {
                ThreadContext.pop();
            }
        }

        private File getSecretsDir() {

            // VT: FIXME: Add sanity check for existence and readability

            return new File(System.getProperty("user.home"), ".dz/oauth/HttpConnector");
        }

        private void processResponse(String rsp) {

            ThreadContext.push("processResponse");
            
            try {
                
                logger.debug("JSON: " + rsp);
                
                Type setType = new TypeToken<Set<ZoneCommand>>(){}.getType();
                Set<ZoneCommand> buffer = gson.fromJson(rsp, setType);
                
                logger.debug("Commands received: " + buffer.size());
                
                if (buffer.isEmpty()) {
                    return;
                }
                
                for (Iterator<ZoneCommand> i = buffer.iterator(); i.hasNext(); ) {
                    
                    executeCommand(i.next());
                }
            
            } finally {
                ThreadContext.pop();
            }
        }

        private void executeCommand(ZoneCommand command) {
            
            ThreadContext.push("executeCommand");
            
            try {
                
                logger.debug("Command: " + command);
                
                for (Iterator<Object> i = getInitSet().iterator(); i.hasNext(); ) {
                    
                    Object next = i.next();
                    
                    if (!(next instanceof ThermostatModel)) {
                        
                        continue;
                    }
                    
                    ThermostatModel ts =  (ThermostatModel) next;
                    
                    if (ts.getName().equals(command.name)) {
                        
                        logger.debug("Matched: " + command.name);
                        
                        ts.setSetpoint(command.setpointTemperature);
                        ts.setOn(command.enabled);
                        ts.setOnHold(command.onHold);
                        ts.setVoting(command.voting);
                    }
                }
                
            } finally {
                ThreadContext.pop();
            }
        }
    }
}
