package com.homeclimatecontrol.hcc.client.rsocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.rsocket.core.RSocketConnector;
import io.rsocket.metadata.TaggingMetadataCodec;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HCC remote client using RSocket protocol.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2024
 */
public class RSocketClient {

    private final Logger logger = LogManager.getLogger();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ZoneStatus> getZones(String bindAddress, int port, String serialization) throws JsonProcessingException {

        var socket = RSocketConnector
                .create()
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
            var zones = objectMapper.readValue(response, Map.class);

            logger.info("ZONES:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(zones));

            return null;
        } finally {
            payload.release();
        }
    }
}
