package net.sf.dz3r.runtime.config;

import net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerSpec;
import net.sf.dz3r.runtime.config.protocol.mqtt.MqttEndpointSpec;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Configuration specific mapper.
 *
 * VT: NOTE: Some methods here have nothing to do with mapstruct, and implement interfaces instead.
 * This might mutate into proper usage if/when components get separated into configuration + implementation pairs,
 * otherwise too many irrelevant dependencies will bleed into this module.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@Mapper
public interface ConfigurationMapper {

    ConfigurationMapper INSTANCE = Mappers.getMapper(ConfigurationMapper.class);

    default MqttEndpointSpec parseEndpoint(MqttBrokerSpec source) {

        return new MqttEndpointSpec() {

            @Override
            public String host() {
                return source.host();
            }

            @Override
            public Integer port() {
                return source.port();
            }

            @Override
            public boolean autoReconnect() {
                return source.autoReconnect();
            }

            @Override
            public String username() {
                return source.username();
            }

            @Override
            public String password() {
                return source.password();
            }

            @Override
            public boolean equals(Object other) {

                if (!(other instanceof MqttEndpointSpec)) {
                    return false;
                }

                return signature().equals(((MqttEndpointSpec) other).signature());
            }

            @Override
            public int hashCode() {
                return signature().hashCode();
            }
        };
    }
}
