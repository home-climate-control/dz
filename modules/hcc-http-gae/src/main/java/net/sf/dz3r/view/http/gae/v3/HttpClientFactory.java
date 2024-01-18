package net.sf.dz3r.view.http.gae.v3;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Creates {@link HttpClient} with custom configuration preventing getting stuck on a request.
 *
 * See <a href="https://github.com/home-climate-control/dz/issues/80">HttpConnector v3 gets stuck without exception</a>.
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

        var clientBuilder = HttpClientBuilder.create();

        // VT: NOTE: Let's try to keep piling connections until the connection can be made,
        // and then let the remote end sort it out

        clientBuilder.setMaxConnPerRoute(100);

        var rcBuilder = RequestConfig.custom();

        // VT: NOTE: It is likely that new connections will be opened every 10 to 40 seconds;
        // if there's no movement in ten seconds, there's likely a bigger problem,
        // or a transient Internet blackout.

        rcBuilder
                .setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000);

        clientBuilder.setDefaultRequestConfig(rcBuilder.build());

        return clientBuilder.build();
    }
}
