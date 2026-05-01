package com.homeclimatecontrol;


import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Creates {@link HttpClient} with custom configuration preventing getting stuck on a request.
 *
 * See <a href="https://github.com/home-climate-control/dz/issues/80">HttpConnector v3 gets stuck without exception</a>.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public class HttpClientFactory {

    /**
     * Convenience method to hold an arbitrarily long chain of builder method invocations.
     *
     * @return Fully built client.
     */
    public static CloseableHttpClient createClient() {

        // VT: NOTE: Let's try to keep piling connections until the connection can be made,
        // and then let the remote end sort it out

        var connectionConfig = ConnectionConfig
                .custom()
                .setConnectTimeout(10, SECONDS)
                .setSocketTimeout(10, SECONDS)
                .build();


        // VT: NOTE: It is likely that new connections will be opened every 10 to 40 seconds;
        // if there's no movement in ten seconds, there's likely a bigger problem,
        // or a transient Internet blackout.

        var requestConfig = RequestConfig
                .custom()
                .setResponseTimeout(10, SECONDS)
                .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder
                .create()
                .setMaxConnPerRoute(100)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        return HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
