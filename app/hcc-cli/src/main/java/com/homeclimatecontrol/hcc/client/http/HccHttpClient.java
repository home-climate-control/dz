package com.homeclimatecontrol.hcc.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;

/**
 * HCC remote client using HTTP protocol.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class HccHttpClient {

    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;

    private synchronized HttpClient getHttpClient() {

        if (httpClient == null) {

            // VT: NOTE: This is about 100ms on a decent workstation. Unexpected.
            // ... but only if it is called from the constructor, otherwise it takes 2ms :O
            httpClient = createClient();
        }

        return httpClient;
    }

    private HttpClient createClient() {

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

            logger.trace("META/raw: {}", response);
            return objectMapper.readValue(response, EndpointMeta.class);

        } finally {
            get.releaseConnection();
        }
    }
}
