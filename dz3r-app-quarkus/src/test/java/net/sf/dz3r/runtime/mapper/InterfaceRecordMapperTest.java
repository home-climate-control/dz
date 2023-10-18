package net.sf.dz3r.runtime.mapper;

import net.sf.dz3r.runtime.config.quarkus.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SwitchConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.FanConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttBrokerConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttDeviceConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InterfaceRecordMapperTest {

    @Test
    void mqttDeviceConfigOptional() {

        var source = new MqttDeviceConfig() {

            @Override
            public MqttBrokerConfig broker() {
                return new MqttBrokerConfig() {
                    @Override
                    public Optional<String> id() {
                        return Optional.empty();
                    }

                    @Override
                    public String host() {
                        return "localhost";
                    }

                    @Override
                    public Optional<Integer> port() {
                        return Optional.of(9999);
                    }

                    @Override
                    public Optional<String> username() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> password() {
                        return Optional.empty();
                    }

                    @Override
                    public String rootTopic() {
                        return "";
                    }


                    @Override
                    public Optional<Boolean> autoReconnect() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public Set<SensorConfig> sensors() {
                return Set.of();
            }

            @Override
            public Set<SwitchConfig> switches() {
                return Set.of();
            }

            @Override
            public Set<FanConfig> fans() {
                return Set.of();
            }
        };

        var result = InterfaceRecordMapper.INSTANCE.mqttConfig(source);

        assertThat(result.broker().id()).isNull();
        assertThat(result.broker().port()).isEqualTo(9999);
        assertThat(result.broker().autoReconnect()).isTrue();
    }
}
