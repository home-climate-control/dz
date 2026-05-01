package com.homeclimatecontrol.hcc.client.rsocket;

import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Map;

/**
 * HCC remote client using RSocket protocol.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public class RSocketClient {

    private final Logger logger = LogManager.getLogger();
    private final JsonMapper jsonMapper;

    public RSocketClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Map<String, ZoneStatus> getZones(String bindAddress, int port, String serialization) throws JacksonException {

        var m = new Marker("getZones");

        try {
            var socket = RSocketConnector
                    .create()
                    .payloadDecoder(PayloadDecoder.ZERO_COPY)
                    .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString())
                    .connect(TcpClientTransport.create(bindAddress, port))
                    .block();

            var routeMetadata =
                    TaggingMetadataCodec
                            .createTaggingContent(ByteBufAllocator.DEFAULT, Collections.singletonList(serialization));

            var payload = socket
                    .requestResponse(
                            ByteBufPayload.create(
                                    ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, "zones"), routeMetadata))
                    .log()
                    .block();

            try {

                var response = payload.getDataUtf8();
                return jsonMapper.readValue(response, Map.class);

            } finally {
                payload.release();
            }

        } finally {
            m.close();
        }
    }
}
