package net.sf.dz3.runtime.mapper;

import net.sf.dz3.runtime.config.HccRawConfig;
import net.sf.dz3.runtime.config.quarkus.HccRawInterfaceConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.SensorConfig;
import net.sf.dz3.runtime.config.quarkus.hardware.SwitchConfig;
import net.sf.dz3.runtime.config.quarkus.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3.runtime.config.quarkus.protocol.onewire.OnewireBusConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Set;

/**
 * SpringBoot loves records. Quarkus <a href="https://github.com/quarkusio/quarkus/issues/32746">is not ready yet</a>. Need to map.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
@Mapper
public interface InterfaceRecordMapper {

    InterfaceRecordMapper INSTANCE = Mappers.getMapper(InterfaceRecordMapper.class);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.esphome()))", target = "esphome")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.zigbee2mqtt()))", target = "zigbee2mqtt")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.zwave2mqtt()))", target = "zwave2mqtt")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.onewire(source.onewire()))", target = "onewire")
    HccRawConfig rawConfig(HccRawInterfaceConfig source);

    Set<net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig> mqtt(Set<MqttDeviceConfig> source);

    Set<net.sf.dz3.runtime.config.protocol.onewire.OnewireBusConfig> onewire(Set<OnewireBusConfig> source);

    Set<net.sf.dz3.runtime.config.hardware.SensorConfig> sensors(Set<SensorConfig> source);

    Set<net.sf.dz3.runtime.config.hardware.SwitchConfig> switches(Set<SwitchConfig> source);

    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.host())", target = "host")
    @Mapping(expression = "java(source.port().orElse(null))", target = "port")
    @Mapping(expression = "java(source.username().orElse(null))", target = "username")
    @Mapping(expression = "java(source.password().orElse(null))", target = "password")
    @Mapping(expression = "java(source.rootTopic())", target = "rootTopic")
    @Mapping(expression = "java(source.autoReconnect().orElse(true))", target = "autoReconnect")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.sensors(source.sensors()))", target = "sensors")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.switches(source.switches()))", target = "switches")
    net.sf.dz3.runtime.config.protocol.mqtt.MqttDeviceConfig mqttConfig(MqttDeviceConfig source);

    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.address())", target = "address")
    @Mapping(expression = "java(source.measurement().orElse(null))", target = "measurement")
    @Mapping(expression = "java(source.step().orElse(null))", target = "step")
    net.sf.dz3.runtime.config.hardware.SensorConfig sensorConfig(SensorConfig source);

    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.address())", target = "address")
    @Mapping(expression = "java(source.reversed().orElse(false))", target = "reversed")
    @Mapping(expression = "java(source.heartbeat().orElse(null))", target = "heartbeat")
    @Mapping(expression = "java(source.pace().orElse(null))", target = "pace")
    net.sf.dz3.runtime.config.hardware.SwitchConfig switchConfig(SwitchConfig source);
}
