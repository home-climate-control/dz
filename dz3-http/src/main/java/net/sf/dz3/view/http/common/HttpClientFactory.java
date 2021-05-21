package net.sf.dz3.view.http.common;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Creates {@link HttpClient} with custom configuration preventing getting stuck on a request.
 *
 * @see https://github.com/home-climate-control/dz/issues/80
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2019
 */
public class HttpClientFactory {

    /**
     * Convenience method to hold an arbitrarily long chain of builder method invocations.
     *
     * @return Fully built client.
     */
    public static HttpClient createClient() {

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        // VT: NOTE: Let's try to keep piling connections until the connection can be made,
        // and then let the remote end sort it out

        clientBuilder = clientBuilder.setMaxConnPerRoute(100);

        Builder rcBuilder = RequestConfig.custom();

        // VT: NOTE: It is likely that new connections will be opened every 10 to 40 seconds;
        // if there's no movement in ten seconds, there's likely a bigger problem,
        // or a transient Internet blackout.

        rcBuilder = rcBuilder.setConnectionRequestTimeout(10 * 1000);
        rcBuilder = rcBuilder.setConnectTimeout(10 * 1000);
        rcBuilder = rcBuilder.setSocketTimeout(10 * 1000);

        clientBuilder.setDefaultRequestConfig(rcBuilder.build());

        return clientBuilder.build();
    }
}
