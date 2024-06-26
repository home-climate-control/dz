package net.sf.dz3r.runtime.config;

import com.homeclimatecontrol.hcc.Version;
import com.homeclimatecontrol.hcc.meta.EndpointMeta;
import com.homeclimatecontrol.hcc.meta.HvacDeviceMeta;
import com.homeclimatecontrol.hcc.meta.InstanceMeta;
import com.homeclimatecontrol.hcc.meta.InstrumentClusterMeta;
import com.homeclimatecontrol.hcc.meta.SimpleClientMeta;
import com.homeclimatecontrol.hcc.meta.ZoneMeta;
import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.runtime.InstanceIdProvider;
import net.sf.dz3r.runtime.config.connector.ConnectorConfig;
import net.sf.dz3r.runtime.config.connector.ConnectorConfigurationParser;
import net.sf.dz3r.runtime.config.connector.HomeAssistantConfig;
import net.sf.dz3r.runtime.config.filter.FilterConfigurationParser;
import net.sf.dz3r.runtime.config.hardware.GenericHvacDeviceConfig;
import net.sf.dz3r.runtime.config.hardware.HvacConfigurationParser;
import net.sf.dz3r.runtime.config.hardware.HvacDeviceConfig;
import net.sf.dz3r.runtime.config.hardware.UnitConfigurationParser;
import net.sf.dz3r.runtime.config.model.ConsoleConfigurationParser;
import net.sf.dz3r.runtime.config.model.DirectorConfigurationParser;
import net.sf.dz3r.runtime.config.model.MeasurementUnits;
import net.sf.dz3r.runtime.config.model.TemperatureUnit;
import net.sf.dz3r.runtime.config.model.WebUiConfigurationParser;
import net.sf.dz3r.runtime.config.model.ZoneConfig;
import net.sf.dz3r.runtime.config.model.ZoneConfigurationParser;
import net.sf.dz3r.runtime.config.mqtt.MqttConfigurationParser;
import net.sf.dz3r.runtime.config.onewire.OnewireConfigurationParser;
import net.sf.dz3r.runtime.config.schedule.ScheduleConfigurationParser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses {@link HccRawConfig} into a live {@link ConfigurationContext}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationParser {

    private final Logger logger = LogManager.getLogger();

    public ConfigurationContext parse(HccRawConfig source, String digest) throws IOException {

        Marker m = new Marker(getClass().getSimpleName() + "#parse", Level.INFO);
        try {

            // Not going anywhere without it, it's a key in too many places
            HCCObjects.requireNonNull(
                    source.instance(),
                    "home-climate-control.instance must be provided. See https://github.com/home-climate-control/dz/tree/master/docs/configuration/index.md for details");

            checkUnits(source.measurementUnits());

            var ctx = new ConfigurationContext();

            var mqtt = new MqttConfigurationParser(ctx)
                    .parse(
                            source.esphome(),
                            source.zigbee2mqtt(),
                            source.zwave2mqtt(),

                            // As usual, HA requires special handling. It needs zones which are not available until much later,
                            // but it also needs an MQTT adapter which will be too late to obtain then - see mqtt.close() right down.
                            getHomeAssistantConfigs(source));

            // There will be no more MQTT adapters after this
            ctx.mqtt.close();

            // VT: FIXME: Add this to the gate when 1-Wire configuration is actually read and parsed
            new OnewireConfigurationParser(ctx).parse(source.onewire()).block();

            // VT: FIXME: Need to resolve XBee sensors and switches
            logger.error("FIXME: NOT IMPLEMENTED: XBee");

            // VT: FIXME: Need to resolve shell sensors and switches
            logger.error("FIXME: NOT IMPLEMENTED: Shell sensors and switches");

            var mocks = new MockConfigurationParser(ctx).parse(source.mocks());

            var gate = Flux.zip(mqtt, mocks);

            gate.blockFirst();
            m.checkpoint("configured sensors and switches");

            // There will be no more switches coming after this
            ctx.switches.close();

            // Need to have all raw sensor feeds resolved by now
            new FilterConfigurationParser(ctx).parse(source.filters());

            // There will be no more sensors coming after this (filters are also "is a" sensors)
            ctx.sensors.close();
            m.checkpoint("configured filters");

            // Need all sensors and switches resolved by now

            new HvacConfigurationParser(ctx).parse(source.hvac());
            ctx.hvacDevices.close();
            m.checkpoint("configured HVAC devices");

            // VT: FIXME: Need to resolve dampers and damper multiplexers

            new ZoneConfigurationParser(ctx).parse(source.zones());
            ctx.zones.close();
            m.checkpoint("configured zones");

            // VT: FIXME: Need to resolve damper controllers, everything is ready for them

            // This may potentially take a long time, we'll close it later right before it's needed
            new ScheduleConfigurationParser(ctx).parse(source.schedule());
            m.checkpoint("configured schedule 1/2");

            var connectorFlux = new ConnectorConfigurationParser(ctx).parse(source.connectors());
            m.checkpoint("configured connectors 1/2");

            new UnitConfigurationParser(ctx).parse(source.units());
            ctx.units.close();
            m.checkpoint("configured units");

            ctx.schedule.close();
            m.checkpoint("configured schedule 2/2");

            connectorFlux.blockLast();
            ctx.collectors.close();
            ctx.connectors.close();
            m.checkpoint("configured connectors 2/2");

            // Need just about everything resolved by now

            new DirectorConfigurationParser(ctx).parse(source.directors());
            ctx.directors.close();
            m.checkpoint("configured directors");

            var ic = new InstrumentCluster(
                    ctx.sensors.getFlux(),
                    ctx.switches.getFlux(),
                    ctx.schedule.getFlux(),
                    ctx.connectors.getFlux(),
                    ctx.collectors.getFlux(),
                    ctx.hvacDevices.getFlux());

            ctx.endpoint.register("endpoint", renderMeta(source, digest));

            // Need directors and the meta resolved by now

            var webUi = new WebUiConfigurationParser(ctx, ic).parse(source.instance(), digest, source.webUi());
            m.checkpoint("configured WebUI");

            var console = new ConsoleConfigurationParser(ctx, ic).parse(source.instance(), source.measurementUnits(), source.console());
            m.checkpoint("configured console");

            if (webUi == null && console == null) {
                // VT: FIXME: Check if an HTTP connector is configured and include that into analysis
                logger.error("Neither WebUI nor console are configured, how are you going to control this? Starting anyway");
            }

            return ctx;

        } finally {
            m.close();
        }
    }

    private void checkUnits(MeasurementUnits units) {

        if (units != null && units.temperature() != TemperatureUnit.C) {
            logger.error("FAHRENHEIT UNITS: NOT THOROUGHLY TESTED. WATCH OUT FOR INCONSISTENCIES. SUBMIT A COMMENT TO https://github.com/home-climate-control/dz/issues/315 IF ANY FOUND");

            throw new UnsupportedOperationException("On second thought... Let's implement it first");
        }
    }

    private EndpointMeta renderMeta(HccRawConfig source, String digest) throws IOException {

        var zones = Flux.fromIterable(source.zones())
                .map(this::renderZoneMeta)
                .collect(Collectors.toSet())
                .block();

        var devices = Flux.fromIterable(source.hvac())
                .flatMap(this::renderHvacMeta)
                .collect(Collectors.toSet())
                .block();

        return new EndpointMeta(
                Version.PROTOCOL_VERSION,
                EndpointMeta.Type.DIRECT,
                new InstanceMeta(
                        source.instance(),
                        InstanceIdProvider.getId(),
                        digest,
                        new SimpleClientMeta(
                                zones,
                                devices
                        ),
                        new InstrumentClusterMeta()
                )
        );
    }

    private ZoneMeta renderZoneMeta(ZoneConfig source) {
        return new ZoneMeta(
                source.id(),
                source.name(),
                source.economizer() != null
        );
    }

    private Flux<HvacDeviceMeta> renderHvacMeta(HvacDeviceConfig source) {

        var result = new HashSet<HvacDeviceMeta>();

        for (var s: Optional.ofNullable(source.switchable()).orElse(Set.of())) {
            result.add(renderHvacDeviceMeta(s));
        }

        for (var s: Optional.ofNullable(source.heatpumpHat()).orElse(Set.of())) {
            result.add(renderHvacDeviceMeta(s));
        }

        for (var s: Optional.ofNullable(source.heatpump()).orElse(Set.of())) {
            result.add(renderHvacDeviceMeta(s));
        }

        for (var s: Optional.ofNullable(source.variable()).orElse(Set.of())) {
            result.add(renderHvacDeviceMeta(s));
        }

        return Flux.fromIterable(result);
    }

    private HvacDeviceMeta renderHvacDeviceMeta(GenericHvacDeviceConfig source) {

        return new HvacDeviceMeta(
                source.id(),
                HvacDeviceMeta.Type.SWITCHABLE,
                source.filter() != null
        );
    }

    private Set<HomeAssistantConfig> getHomeAssistantConfigs(HccRawConfig source) {

        return Optional.ofNullable(source.connectors())
                .map(c -> Flux
                        .fromIterable(c)
                        .filter(cc -> cc.homeAssistant() != null)
                        .map(ConnectorConfig::homeAssistant))
                .orElse(Flux.empty())
                .collect(Collectors.toSet())
                .block();
    }
}
