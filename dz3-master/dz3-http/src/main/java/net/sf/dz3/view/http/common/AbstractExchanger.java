package net.sf.dz3.view.http.common;

import java.net.URL;
import java.util.concurrent.BlockingQueue;

import net.sf.dz3.view.http.v1.HttpConnector;
import net.sf.jukebox.service.ActiveService;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

/**
 * The facilitator between the client {@link #send(Object) sending} data and the server
 * possibly returning some.
 * 
 * @param <DataBlock> Data type to send out to the server.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2011
 */
public abstract class AbstractExchanger<DataBlock> extends ActiveService {
    
    protected final HttpClient httpClient = new HttpClient();

    protected final URL serverContextRoot;
    private String username;
    private String password;

    protected final BlockingQueue<DataBlock> upstreamQueue;
    
    public AbstractExchanger(URL serverContextRoot, String username, String password, BlockingQueue<DataBlock> upstreamQueue) {
        
        this.serverContextRoot = serverContextRoot;
        this.upstreamQueue = upstreamQueue;
        this.username = username;
        this.password = password;
    }

    @Override
    protected void startup() throws Throwable {

        logger.info("Using " + serverContextRoot);

        // Do absolutely nothing
        
        // Except, maybe, authenticate
        authenticate();
    }

    /**
     * Keep sending data that appears in {@link HttpConnector#upstreamQueue} to the server,
     * and accepting whatever they have to say.
     * 
     * Exact strategy is determined by the implementation subclass.
     */
    protected abstract void execute() throws Throwable;

    @Override
    protected void shutdown() throws Throwable {

        // Do absolutely nothing
        // VT: FIXME: Tell the server that we're gone and invalidate the session?
    }
    
    private void authenticate() {
        
        AuthScope authscope = new AuthScope(serverContextRoot.getHost(), serverContextRoot.getPort());
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(authscope, credentials);
    }
}
