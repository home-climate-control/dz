package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.protocol.mqtt.ESPHomeListenerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationParserTest {


    @ParameterizedTest
    @MethodSource("hostPortTopicProvider")
    void testHostPort(HostPortTopic source) {

        var parser = new ConfigurationParser();

        var result = parser.renderMqttUrl(
                new ESPHomeListenerConfig(
                        null,
                        source.host,
                        source.port,
                        null,
                        null,
                        source.topic,
                        true,
                        null,
                        null));

        assertThat(result).isEqualTo(source.expected);

    }

    public static Stream<HostPortTopic> hostPortTopicProvider() {

        return Stream.of(
                new HostPortTopic("a", 1, "/", "mqtt://a:1//"),
                new HostPortTopic("b", null, "#", "mqtt://b:1883/#"),
                new HostPortTopic(null, 2, null, "mqtt://localhost:2/"),
                new HostPortTopic(null, null, null, "mqtt://localhost:1883/")
        );
    }

    public record HostPortTopic(String host, Integer port, String topic, String expected) {
    }
}
