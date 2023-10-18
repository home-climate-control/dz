package net.sf.dz3r.runtime.mapper;

import net.sf.dz3r.runtime.config.HccRawConfig;
import net.sf.dz3r.runtime.config.quarkus.HccRawInterfaceConfig;
import net.sf.dz3r.runtime.config.quarkus.connector.ConnectorConfig;
import net.sf.dz3r.runtime.config.quarkus.connector.HomeAssistantConfig;
import net.sf.dz3r.runtime.config.quarkus.connector.HttpConnectorConfig;
import net.sf.dz3r.runtime.config.quarkus.connector.InfluxCollectorConfig;
import net.sf.dz3r.runtime.config.quarkus.filter.FilterConfig;
import net.sf.dz3r.runtime.config.quarkus.filter.MedianFilterConfig;
import net.sf.dz3r.runtime.config.quarkus.filter.MedianSetFilterConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.HeatpumpConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.HeatpumpHATConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.HvacDeviceConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.MockConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.MultiStageUnitControllerConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SensorConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SingleStageUnitControllerConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SwitchConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.SwitchableHvacDeviceConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.UnitControllerConfig;
import net.sf.dz3r.runtime.config.quarkus.hardware.VariableHvacConfig;
import net.sf.dz3r.runtime.config.quarkus.model.ConsoleConfig;
import net.sf.dz3r.runtime.config.quarkus.model.EconomizerConfig;
import net.sf.dz3r.runtime.config.quarkus.model.PidControllerConfig;
import net.sf.dz3r.runtime.config.quarkus.model.RangeConfig;
import net.sf.dz3r.runtime.config.quarkus.model.UnitDirectorConfig;
import net.sf.dz3r.runtime.config.quarkus.model.WebUiConfig;
import net.sf.dz3r.runtime.config.quarkus.model.ZoneConfig;
import net.sf.dz3r.runtime.config.quarkus.model.ZoneSettingsConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.FanConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttBrokerConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.mqtt.MqttDeviceConfig;
import net.sf.dz3r.runtime.config.quarkus.protocol.onewire.OnewireBusConfig;
import net.sf.dz3r.runtime.config.quarkus.schedule.CalendarConfigEntry;
import net.sf.dz3r.runtime.config.quarkus.schedule.ScheduleConfig;
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

    @Mapping(expression = "java(source.instance())", target = "instance")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.esphome()))", target = "esphome")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.zigbee2mqtt()))", target = "zigbee2mqtt")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mqtt(source.zwave2mqtt()))", target = "zwave2mqtt")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.onewire(source.onewire()))", target = "onewire")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.mocks(source.mocks()))", target = "mocks")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.filters(source.filters()))", target = "filters")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.zones(source.zones()))", target = "zones")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.schedule(source.schedule()))", target = "schedule")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.connectors(source.connectors()))", target = "connectors")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.hvac(source.hvac()))", target = "hvac")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.units(source.units()))", target = "units")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.directors(source.directors()))", target = "directors")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.webUi(source.webUi().orElse(null)))", target = "webUi")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.console(source.console().orElse(null)))", target = "console")
    HccRawConfig rawConfig(HccRawInterfaceConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.broker(source.broker()))", target = "broker")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.sensors(source.sensors()))", target = "sensors")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.switches(source.switches()))", target = "switches")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.fans(source.fans()))", target = "fans")
    net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig mqttConfig(MqttDeviceConfig source);

    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.host())", target = "host")
    @Mapping(expression = "java(source.port().orElse(null))", target = "port")
    @Mapping(expression = "java(source.username().orElse(null))", target = "username")
    @Mapping(expression = "java(source.password().orElse(null))", target = "password")
    @Mapping(expression = "java(source.rootTopic())", target = "rootTopic")
    @Mapping(expression = "java(source.autoReconnect().orElse(true))", target = "autoReconnect")
    net.sf.dz3r.runtime.config.protocol.mqtt.MqttBrokerConfig broker(MqttBrokerConfig source);


    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.address())", target = "address")
    @Mapping(expression = "java(source.measurement().orElse(null))", target = "measurement")
    @Mapping(expression = "java(source.step().orElse(null))", target = "step")
    @Mapping(expression = "java(source.timeout().orElse(null))", target = "timeout")
    net.sf.dz3r.runtime.config.hardware.SensorConfig sensorConfig(SensorConfig source);

    @Mapping(expression = "java(source.id().orElse(null))", target = "id")
    @Mapping(expression = "java(source.address())", target = "address")
    @Mapping(expression = "java(source.reversed().orElse(false))", target = "reversed")
    @Mapping(expression = "java(source.heartbeat().orElse(null))", target = "heartbeat")
    @Mapping(expression = "java(source.pace().orElse(null))", target = "pace")
    @Mapping(expression = "java(source.optimistic().orElse(null))", target = "optimistic")
    net.sf.dz3r.runtime.config.hardware.SwitchConfig switchConfig(SwitchConfig source);

    @Mapping(expression = "java(source.serialPort())", target = "serialPort")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.sensors(source.sensors()))", target = "sensors")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.switches(source.switches()))", target = "switches")
    net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig onewire(OnewireBusConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.switches(source.switches()))", target = "switches")
    net.sf.dz3r.runtime.config.hardware.MockConfig mock(MockConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.median(source.median()))", target = "median")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.medianSet(source.medianSet()))", target = "medianSet")
    net.sf.dz3r.runtime.config.filter.FilterConfig filter(FilterConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.depth())", target = "depth")
    @Mapping(expression = "java(source.source())", target = "source")
    net.sf.dz3r.runtime.config.filter.MedianFilterConfig median(MedianFilterConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.sources())", target = "sources")
    net.sf.dz3r.runtime.config.filter.MedianSetFilterConfig medianSet(MedianSetFilterConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.name())", target = "name")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.controller(source.controller()))", target = "controller")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.settings(source.settings()))", target = "settings")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.economizer(source.economizer().orElse(null)))", target = "economizer")
    net.sf.dz3r.runtime.config.model.ZoneConfig zone(ZoneConfig source);

    @Mapping(expression = "java(source.p())", target = "p")
    @Mapping(expression = "java(source.i())", target = "i")
    @Mapping(expression = "java(source.d())", target = "d")
    @Mapping(expression = "java(source.limit())", target = "limit")
    net.sf.dz3r.runtime.config.model.PidControllerConfig controller(PidControllerConfig source);

    @Mapping(expression = "java(source.enabled().orElse(true))", target = "enabled")
    @Mapping(expression = "java(source.setpoint())", target = "setpoint")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.range(source.setpointRange().orElse(null)))", target = "setpointRange")
    @Mapping(expression = "java(source.voting().orElse(true))", target = "voting")
    @Mapping(expression = "java(source.hold().orElse(false))", target = "hold")
    @Mapping(expression = "java(source.dumpPriority().orElse(null))", target = "dumpPriority")
    net.sf.dz3r.runtime.config.model.ZoneSettingsConfig settings(ZoneSettingsConfig source);

    @Mapping(expression = "java(source.ambientSensor())", target = "ambientSensor")
    @Mapping(expression = "java(source.changeoverDelta())", target = "changeoverDelta")
    @Mapping(expression = "java(source.targetTemperature())", target = "targetTemperature")
    @Mapping(expression = "java(source.keepHvacOn())", target = "keepHvacOn")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.controller(source.controller()))", target = "controller")
    @Mapping(expression = "java(source.mode())", target = "mode")
    @Mapping(expression = "java(source.hvacDevice())", target = "hvacDevice")
    net.sf.dz3r.runtime.config.model.EconomizerConfig economizer(EconomizerConfig source);

    @Mapping(expression = "java(source.min())", target = "min")
    @Mapping(expression = "java(source.max())", target = "max")
    net.sf.dz3r.runtime.config.model.RangeConfig range(RangeConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.googleCalendar(source.googleCalendar()))", target = "googleCalendar")
    net.sf.dz3r.runtime.config.schedule.ScheduleConfig schedule(ScheduleConfig source);

    @Mapping(expression = "java(source.zone())", target = "zone")
    @Mapping(expression = "java(source.calendar())", target = "calendar")
    net.sf.dz3r.runtime.config.schedule.CalendarConfigEntry calendarConfigEntry(CalendarConfigEntry source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.connector(source.http().orElse(null)))", target = "http")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.connector(source.influx().orElse(null)))", target = "influx")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.connector(source.homeAssistant().orElse(null)))", target = "homeAssistant")
    net.sf.dz3r.runtime.config.connector.ConnectorConfig connector(ConnectorConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.uri())", target = "uri")
    @Mapping(expression = "java(source.zones())", target = "zones")
    net.sf.dz3r.runtime.config.connector.HttpConnectorConfig connector(HttpConnectorConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.instance())", target = "instance")
    @Mapping(expression = "java(source.db())", target = "db")
    @Mapping(expression = "java(source.uri())", target = "uri")
    @Mapping(expression = "java(source.username().orElse(null))", target = "username")
    @Mapping(expression = "java(source.password().orElse(null))", target = "password")
    @Mapping(expression = "java(source.sensorFeedMapping())", target = "sensorFeedMapping")
    net.sf.dz3r.runtime.config.connector.InfluxCollectorConfig connector(InfluxCollectorConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.broker(source.broker()))", target = "broker")
    @Mapping(expression = "java(source.discoveryPrefix().orElse(null))", target = "discoveryPrefix")
    @Mapping(expression = "java(source.zones())", target = "zones")
    net.sf.dz3r.runtime.config.connector.HomeAssistantConfig connector(HomeAssistantConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.switchable(source.switchable()))", target = "switchable")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.heatpumpHat(source.heatpumpHat()))", target = "heatpumpHat")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.heatpump(source.heatpump()))", target = "heatpump")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.variable(source.variable()))", target = "variable")
    net.sf.dz3r.runtime.config.hardware.HvacDeviceConfig hvac(HvacDeviceConfig source);

    @Mapping(expression = "java(source.lifetime())", target = "lifetime")
    net.sf.dz3r.runtime.config.hardware.FilterConfig filter(net.sf.dz3r.runtime.config.quarkus.hardware.FilterConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.mode())", target = "mode")
    @Mapping(expression = "java(source.switchAddress())", target = "switchAddress")
    @Mapping(expression = "java(source.switchReverse().orElse(null))", target = "switchReverse")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.filter(source.filter().orElse(null)))", target = "filter")
    net.sf.dz3r.runtime.config.hardware.SwitchableHvacDeviceConfig switchable(SwitchableHvacDeviceConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.modeChangeDelay().orElse(null))", target = "modeChangeDelay")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.filter(source.filter().orElse(null)))", target = "filter")
    net.sf.dz3r.runtime.config.hardware.HeatpumpHATConfig heatpumpHat(HeatpumpHATConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.switchMode())", target = "switchMode")
    @Mapping(expression = "java(source.switchModeReverse().orElse(null))", target = "switchModeReverse")
    @Mapping(expression = "java(source.switchRunning())", target = "switchRunning")
    @Mapping(expression = "java(source.switchRunningReverse().orElse(null))", target = "switchRunningReverse")
    @Mapping(expression = "java(source.switchFan())", target = "switchFan")
    @Mapping(expression = "java(source.switchFanReverse().orElse(null))", target = "switchFanReverse")
    @Mapping(expression = "java(source.modeChangeDelay().orElse(null))", target = "modeChangeDelay")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.filter(source.filter().orElse(null)))", target = "filter")
    net.sf.dz3r.runtime.config.hardware.HeatpumpConfig heatpump(HeatpumpConfig source);

    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.singleStage(source.singleStage()))", target = "singleStage")
    @Mapping(expression = "java(InterfaceRecordMapper.INSTANCE.multiStage(source.multiStage()))", target = "multiStage")
    net.sf.dz3r.runtime.config.hardware.UnitControllerConfig unit(UnitControllerConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    net.sf.dz3r.runtime.config.hardware.SingleStageUnitControllerConfig singleStage(SingleStageUnitControllerConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.stages())", target = "stages")
    net.sf.dz3r.runtime.config.hardware.MultiStageUnitControllerConfig singleStage(MultiStageUnitControllerConfig source);

    @Mapping(expression = "java(source.id())", target = "id")
    @Mapping(expression = "java(source.connectors())", target = "connectors")
    @Mapping(expression = "java(source.sensorFeedMapping())", target = "sensorFeedMapping")
    @Mapping(expression = "java(source.unit())", target = "unit")
    @Mapping(expression = "java(source.hvac())", target = "hvac")
    @Mapping(expression = "java(source.mode())", target = "mode")
    net.sf.dz3r.runtime.config.model.UnitDirectorConfig director(UnitDirectorConfig source);
    @Mapping(expression = "java(source.port().orElse(null))", target = "port")
    @Mapping(expression = "java(source.interfaces().orElse(null))", target = "interfaces")
    @Mapping(expression = "java(source.directors().orElse(null))", target = "directors")
    @Mapping(expression = "java(source.units().orElse(null))", target = "units")
    net.sf.dz3r.runtime.config.model.WebUiConfig webUi(WebUiConfig source);

    @Mapping(expression = "java(source.units().orElse(null))", target = "units")
    @Mapping(expression = "java(source.directors().orElse(Set.of()))", target = "directors")
    @Mapping(expression = "java(source.sensors().orElse(Set.of()))", target = "sensors")
    net.sf.dz3r.runtime.config.model.ConsoleConfig console(ConsoleConfig source);

    Set<net.sf.dz3r.runtime.config.hardware.SensorConfig> sensors(Set<SensorConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.SwitchConfig> switches(Set<SwitchConfig> source);
    Set<net.sf.dz3r.runtime.config.protocol.mqtt.FanConfig> fans(Set<FanConfig> source);
    Set<net.sf.dz3r.runtime.config.protocol.mqtt.MqttDeviceConfig> mqtt(Set<MqttDeviceConfig> source);
    Set<net.sf.dz3r.runtime.config.protocol.onewire.OnewireBusConfig> onewire(Set<OnewireBusConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.MockConfig> mocks(Set<MockConfig> source);
    Set<net.sf.dz3r.runtime.config.filter.FilterConfig> filters(Set<FilterConfig> source);
    Set<net.sf.dz3r.runtime.config.filter.MedianFilterConfig> median(Set<MedianFilterConfig> source);
    Set<net.sf.dz3r.runtime.config.filter.MedianSetFilterConfig> medianSet(Set<MedianSetFilterConfig> source);
    Set<net.sf.dz3r.runtime.config.model.ZoneConfig> zones(Set<ZoneConfig> source);
    Set<net.sf.dz3r.runtime.config.schedule.CalendarConfigEntry> googleCalendar(Set<CalendarConfigEntry> source);
    Set<net.sf.dz3r.runtime.config.connector.ConnectorConfig> connectors(Set<ConnectorConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.HvacDeviceConfig> hvac(Set<HvacDeviceConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.SwitchableHvacDeviceConfig> switchable(Set<SwitchableHvacDeviceConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.HeatpumpHATConfig> heatpumpHat(Set<HeatpumpHATConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.HeatpumpConfig> heatpump(Set<HeatpumpConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.VariableHvacConfig> variable(Set<VariableHvacConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.UnitControllerConfig> units(Set<UnitControllerConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.SingleStageUnitControllerConfig> singleStage(Set<SingleStageUnitControllerConfig> source);
    Set<net.sf.dz3r.runtime.config.hardware.MultiStageUnitControllerConfig> multiStage(Set<MultiStageUnitControllerConfig> source);
    Set<net.sf.dz3r.runtime.config.model.UnitDirectorConfig> directors(Set<UnitDirectorConfig> source);
}
