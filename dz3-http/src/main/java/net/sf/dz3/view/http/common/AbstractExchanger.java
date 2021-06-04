package net.sf.dz3.view.http.common;

import com.homeclimatecontrol.jukebox.service.ActiveService;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.ThreadContext;

import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * The facilitator between the client sending data and the server possibly returning some.
 *
 * @param <T> Data type to send out to the server.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractExchanger<T> extends ActiveService {

    protected final HttpClient httpClient = HttpClientFactory.createClient();
    protected final HttpClientContext context = HttpClientContext.create();

    protected final URL serverContextRoot;
    private final String username;
    private final String password;

    protected final BlockingQueue<T> upstreamQueue;

    protected AbstractExchanger(URL serverContextRoot, String username, String password, BlockingQueue<T> upstreamQueue) {

        this.serverContextRoot = serverContextRoot;
        this.upstreamQueue = upstreamQueue;
        this.username = username;
        this.password = password;
    }

    @Override
    protected void startup() throws Throwable {

        logger.info("Using {}", serverContextRoot);

        // Do absolutely nothing

        // Except, maybe, authenticate
        authenticate();
    }

    /**
     * Keep sending data that appears in {@code HttpConnector#upstreamQueue} to the server,
     * and accepting whatever they have to say.
     *
     * Exact strategy is determined by the implementation subclass.
     */
    @Override
    protected abstract void execute() throws Throwable;

    @Override
    protected void shutdown() throws Throwable {

        // Do absolutely nothing
        // VT: FIXME: Tell the server that we're gone and invalidate the session?
    }

    private void authenticate() {

        ThreadContext.push("authenticate");

        try {

            if (username == null || password == null) {

                logger.warn("username or password are null, skipping auth setup");
                return;
            }

            var targetHost = new HttpHost(
                    serverContextRoot.getHost(),
                    serverContextRoot.getPort(),
                    serverContextRoot.getProtocol());

            var credsProvider = new BasicCredentialsProvider();
            var credentials = new UsernamePasswordCredentials(username, password);

            credsProvider.setCredentials(AuthScope.ANY, credentials);

            var authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());

            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);

        } finally {
            ThreadContext.pop();
        }
    }
}
