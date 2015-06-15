package net.sf.dz3.view.http.v1;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.dz3.view.http.common.AbstractExchanger;
import net.sf.dz3.view.http.common.ImmediateExchanger;
import net.sf.jukebox.jmx.JmxDescriptor;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.NDC;

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
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2011
 */
public class HttpConnector extends Connector<RestRenderer> {
    
    private final BlockingQueue<UpstreamBlock> upstreamQueue = new LinkedBlockingQueue<UpstreamBlock>();
    private final URL serverContextRoot;
    private final AbstractExchanger<UpstreamBlock> exchanger;

    /**
     * Create an instance and fill it up with objects to render.
     * 
     * @param initSet Objects to display.
     */
    public HttpConnector(URL serverContextRoot, String username, String password, Set<Object> initSet) {

        super(initSet);
        
        this.serverContextRoot = serverContextRoot;
        
        exchanger = new UpstreamBlockExchanger(serverContextRoot, username, password, upstreamQueue);
        
        register(AnalogSensor.class, new SensorFactory());
        register(ThermostatModel.class, new ThermostatFactory());
    }

    /**
     * Create an instance and fill it up with objects to render,
     * using custom factory set.
     * 
     * @param initSet Objects to display.
     * @param factorySet Set of {@link ConnectorFactory} objects to use for component creation.
     */
    public HttpConnector(URL serverBase, String username, String password, Set<Object> initSet, Set<ConnectorFactory<RestRenderer>> factorySet) {
        
        super(initSet, factorySet);
        
        this.serverContextRoot = serverBase;

        exchanger = new UpstreamBlockExchanger(serverContextRoot, username, password, upstreamQueue);
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
                "HTTP Client v1");
    }
    
    @Override
    protected void activate2() {
        
        exchanger.start();
    }

    @Override
    protected Map<String, Object> createContext() {

        Map<String, Object> context = new TreeMap<String, Object>();
        
        context.put("upstream queue", upstreamQueue);
        return context;
    }

    @Override
    protected void deactivate2() {
        
        exchanger.stop();
    }
    
    private static class UpstreamBlockExchanger extends ImmediateExchanger<UpstreamBlock> {

        public UpstreamBlockExchanger(URL serverContextRoot, String username, String password, BlockingQueue<UpstreamBlock> upstreamQueue) {

            super(serverContextRoot, username, password, upstreamQueue);
        }

        @Override
        protected void send(UpstreamBlock dataBlock) throws IOException {
            
            NDC.push("send");
            
            try {

                logger.debug("Sending " + dataBlock);
                
                URL targetUrl = new URL(serverContextRoot, dataBlock.path);
                
                logger.debug("URL: " + targetUrl);
                
                PostMethod post = new PostMethod(targetUrl.toString());
                post.setDoAuthentication(true);
                
                for (Iterator<String> i = dataBlock.stateMap.keySet().iterator(); i.hasNext(); ) {
                    
                    String name = i.next();
                    String value = dataBlock.stateMap.get(name);

                    post.addParameter(name, value);
                }
                
                try {

                    int rc = httpClient.executeMethod(post);

                    if (rc != 200) {

                        logger.error("HTTP rc=" + rc + ", text follows:");
                        logger.error(post.getResponseBodyAsString());
                        
                        throw new IOException("Request failed with HTTP code " + rc);
                    }
                    
                } finally {
                    post.releaseConnection();
                }
                
            } finally {
                NDC.pop();
            }
        }
    }
}
