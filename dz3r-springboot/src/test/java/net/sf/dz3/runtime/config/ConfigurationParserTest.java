package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationParserTest {


    @ParameterizedTest
    @MethodSource("mqttEndpointExpectedProvider")
    void testHostPort(MqttEndpointExpected source) {

        var spec = new MqttDeviceConfig(
                        null,
                        source.host,
                        source.port,
                        null,
                        null,
                        "none",
                        source.autoReconnect,
                        null,
                        null);

        assertThat(spec.signature()).isEqualTo(source.expected);

    }

    public static Stream<MqttEndpointExpected> mqttEndpointExpectedProvider() {

        return Stream.of(
                new MqttEndpointExpected("a", 1, true, "mqtt://a:1,autoReconnect=true,rootTopic=none"),
                new MqttEndpointExpected("b", null, false, "mqtt://b:1883,autoReconnect=false,rootTopic=none"),
                new MqttEndpointExpected(null, 2, true, "mqtt://localhost:2,autoReconnect=true,rootTopic=none"),
                new MqttEndpointExpected(null, null, false, "mqtt://localhost:1883,autoReconnect=false,rootTopic=none")
        );
    }

    public record MqttEndpointExpected(String host, Integer port, boolean autoReconnect, String expected) {
    }
}
