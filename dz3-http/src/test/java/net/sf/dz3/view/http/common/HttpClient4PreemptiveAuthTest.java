package net.sf.dz3.view.http.common;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

public class HttpClient4PreemptiveAuthTest {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Test target.
     * 
     * Must support HTTP Basic Authentication, and accept the configured {@link #USERNAME} and {@link #PASSWORD}.
     */
    private final String TARGET_URL = "test.httpclient4auth.url";

    private final String USERNAME = "test.httpclient4auth.username";
    private final String PASSWORD = "test.httpclient4auth.password";

    @Test
    public void testPreemptiveAuth() throws IOException {

        ThreadContext.push("testPreemptiveAuth");

        try {

            if (!ContextChecker.runNow(logger)) {
                return;
            }

            Map<String, String> env = ContextChecker.check(new String[] {TARGET_URL, USERNAME, PASSWORD}, true);

            String targetUrl = env.get(TARGET_URL);
            String username = env.get(USERNAME);
            String password = env.get(PASSWORD);

            logger.info("Connecting to " + targetUrl);

            // VT: NOTE: It is assumed that this will not run in unsecure environment and/or that
            // target URL auth is test only and is not sensitive

            logger.info("Will authenticate as " + username + ":" + password);

            callUnauthenticated(targetUrl);
            callAuthenticated(targetUrl, username, password);

        } finally {
            ThreadContext.pop();
        }
    }

    private void callUnauthenticated(String targetUrl) throws IOException {

        ThreadContext.push("callUnauthenticated");

        try {

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(targetUrl);

            HttpResponse rsp = client.execute(get);
            int rc = rsp.getStatusLine().getStatusCode();

            logger.info("RC=" + rc);

            assertEquals("wrong unauthenticated call response code", 401, rc);

        } finally {
            ThreadContext.pop();
        }
    }

    private void callAuthenticated(String targetUrl, String username, String password) throws IOException {

        ThreadContext.push("callAuthenticated");

        try {

            URL target = new URL(targetUrl);
            HttpClientContext context = setUpAuth(target, username, password);

            HttpClient client = HttpClientBuilder.create().build();
            
            {
                HttpGet get = new HttpGet(targetUrl);

                HttpResponse rsp = client.execute(get, context);
                int rc = rsp.getStatusLine().getStatusCode();

                logger.info("RC=" + rc);
                
                assertEquals("wrong auth setup call response code", 200, rc);
            }

            {
                HttpGet get = new HttpGet(targetUrl);

                HttpResponse rsp = client.execute(get, context);
                int rc = rsp.getStatusLine().getStatusCode();

                logger.info("RC=" + rc);
                
                assertEquals("wrong second call response code", 200, rc);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    private HttpClientContext setUpAuth(URL target, String username, String password) {

        ThreadContext.push("setUpAuth");

        try {

            HttpHost targetHost = new HttpHost(
                    target.getHost(),
                    target.getPort(),
                    target.getProtocol());
            
            logger.info("targetHost: " + targetHost);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(username, password);

            credsProvider.setCredentials(AuthScope.ANY, credentials);

            AuthCache authCache = new BasicAuthCache();
            authCache.put(targetHost, new BasicScheme());

            HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(credsProvider);
            context.setAuthCache(authCache);
            
            return context;

        } finally {
            ThreadContext.pop();
        }
    }
}
