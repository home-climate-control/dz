package net.sf.dz3.view.http.common;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.view.http.v1.HttpConnector;
import net.sf.jukebox.service.ActiveService;

/**
 * The facilitator between the client {@link #send(Object) sending} data and the server
 * possibly returning some.
 * 
 * @param <DataBlock> Data type to send out to the server.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public abstract class AbstractExchanger<DataBlock> extends ActiveService {
    
    protected final HttpClient httpClient = HttpClientFactory.createClient();
    protected final HttpClientContext context = HttpClientContext.create();

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

    private void authenticate() throws IOException {

        ThreadContext.push("authenticate");

        try {

            if (username == null || password == null) {

                logger.warn("username or password are null, skipping auth setup");
                return;
            }

            HttpHost targetHost = new HttpHost(
                    serverContextRoot.getHost(),
                    serverContextRoot.getPort(),
                    serverContextRoot.getProtocol());
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(username, password);

            credsProvider.setCredentials(AuthScope.ANY, credentials);

            AuthCache authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());

            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);

        } finally {
            ThreadContext.pop();
        }
    }
}
