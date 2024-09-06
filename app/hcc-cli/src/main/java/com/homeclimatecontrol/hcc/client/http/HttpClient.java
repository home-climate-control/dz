package com.homeclimatecontrol.hcc.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeclimatecontrol.hcc.ClientBootstrap;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * HCC remote client using HTTP protocol.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class HttpClient {

    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper;

    private org.apache.http.client.HttpClient httpClient;

    public HttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private synchronized org.apache.http.client.HttpClient getHttpClient() {

        if (httpClient == null) {

            // VT: NOTE: This is about 100ms on a decent workstation. Unexpected.
            // ... but only if it is called from the constructor, otherwise it takes 2ms :O
            httpClient = createClient();
        }

        return httpClient;
    }

    private org.apache.http.client.HttpClient createClient() {

        // VT: NOTE: Copypasted with abbreviations from HttpClientFactory (search the whole project).
        // Important fact not to forget: https://github.com/home-climate-control/dz/issues/80

        return HttpClientBuilder
                .create()
                .setMaxConnPerRoute(100)
                .setDefaultRequestConfig(
                        RequestConfig
                                .custom()
                                .setConnectionRequestTimeout(10 * 1000)
                                .setConnectTimeout(10 * 1000)
                                .setSocketTimeout(10 * 1000)
                                .build()
                )
                .build();
    }

    public EndpointMeta getMeta(URL targetUrl) throws IOException {

        return objectMapper.readValue(get(targetUrl, "getMeta"), EndpointMeta.class);
    }

    public Map<String, ZoneStatus> getZones(URL targetUrl) throws IOException {

        // VT: FIXME: This returns a map of maps :O Will deal with this in a short bit.
        return objectMapper.readValue(get(targetUrl, "getZones"), Map.class);
    }

    public ClientBootstrap getBootstrap(URL targetUrl) throws IOException {

        return objectMapper.readValue(get(targetUrl, "getBootstrap"), ClientBootstrap.class);
    }

    private String get(URL targetUrl, String marker) throws IOException {

        var m = new Marker(marker);
        var get = new HttpGet(targetUrl.toString());

        try {

            var rsp = getHttpClient().execute(get);
            var rc = rsp.getStatusLine().getStatusCode();

            if (rc != 200) {

                logger.error("HTTP rc={}, text follows:", rc);
                logger.error(EntityUtils.toString(rsp.getEntity())); // NOSONAR Not worth the effort

                throw new IOException("Request to " + targetUrl + " failed with HTTP code " + rc);
            }

            var response = EntityUtils.toString(rsp.getEntity());

            logger.trace("{}/raw: {}", marker, response);

            return response;

        } finally {
            get.releaseConnection();
            m.close();
        }
    }
}
