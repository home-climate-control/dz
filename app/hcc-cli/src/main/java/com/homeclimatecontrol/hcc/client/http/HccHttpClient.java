package com.homeclimatecontrol.hcc.client.http;

import com.homeclimatecontrol.HttpClientFactory;
import com.homeclimatecontrol.hcc.ClientBootstrap;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * HCC remote client using HTTP protocol.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public class HccHttpClient {

    private final Logger logger = LogManager.getLogger();
    private final JsonMapper jsonMapper;

    private CloseableHttpClient httpClient;

    public HccHttpClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    private synchronized CloseableHttpClient getHttpClient() {

        if (httpClient == null) {

            // VT: NOTE: This is about 100ms on a decent workstation. Unexpected.
            // ... but only if it is called from the constructor, otherwise it takes 2ms :O
            httpClient = HttpClientFactory.createClient();
        }

        return httpClient;
    }


    public EndpointMeta getMeta(URL targetUrl) throws IOException {

        return jsonMapper.readValue(get(targetUrl, "getMeta"), EndpointMeta.class);
    }

    public Map<String, ZoneStatus> getZones(URL targetUrl) throws IOException {

        // VT: FIXME: This returns a map of maps :O Will deal with this in a short bit.
        return jsonMapper.readValue(get(targetUrl, "getZones"), Map.class);
    }

    public ClientBootstrap getBootstrap(URL targetUrl) throws IOException {

        return jsonMapper.readValue(get(targetUrl, "getBootstrap"), ClientBootstrap.class);
    }

    private String get(URL targetUrl, String marker) throws IOException {

        var m = new Marker(marker);
        var get = new HttpGet(targetUrl.toString());

        try (var rsp = getHttpClient().execute(get)) {

            var rc = rsp.getCode();
            var entity = rsp.getEntity();

            try {
                if (rc != 200) {

                    logger.error("HTTP rc={}, text follows:", rc);
                    logger.error(EntityUtils.toString(rsp.getEntity())); // NOSONAR Not worth the effort

                    throw new IOException("Request to " + targetUrl + " failed with HTTP code " + rc);
                }

                var response = EntityUtils.toString(rsp.getEntity());

                logger.trace("{}/raw: {}", marker, response);

                return response;

            } catch (ParseException ex) {
                throw new IOException("Failed to parse response entity: " + entity, ex);
            }
        } finally {
            m.close();
        }
    }
}
