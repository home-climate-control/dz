package net.sf.dz3.runtime.config;

import net.sf.dz3.runtime.config.hardware.ESPHomeListenerConfig;
import net.sf.dz3.runtime.config.hardware.OnewireBusConfig;
import net.sf.dz3.runtime.config.hardware.Z2MJsonListenerConfig;
import net.sf.dz3.runtime.config.hardware.ZWaveListenerConfig;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Parses {@link HccRawConfig} into {@link HccParsedConfig}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationParser {
    private final Logger logger = LogManager.getLogger();

    public HccParsedConfig parse(HccRawConfig source) {

        var sensors = Flux
                .just(
                        new EspSensorFluxResolver(source.esphome()),
                        new OnewireSensorFluxResolver(source.onewire()),
                        new ZigbeeSensorFluxResolver(source.zigbee2mqtt()),
                        new ZWaveSensorFluxResolver(source.zwave2mqtt())
                )
                .map(SensorFluxResolver::getSensorFluxes)
                .blockLast();



        logger.error("ConfigurationParser::parse(): NOT IMPLEMENTED");
        return new HccParsedConfig();
    }

    private abstract class SensorFluxResolver<T> {

        protected final List<T> source;

        protected SensorFluxResolver(List<T> source) {
            this.source = source;
        }

        /**
         * Parse the configuration into the mapping from the flux ID to the flux.
         *
         * @return Map of (flux ID, flux) for all the given sources.
         */
        protected abstract Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<T> source);

        public final Map<String, Flux<Signal<Double, Void>>> getSensorFluxes() {
            logger.error("NOT IMPLEMENTED: {}#getSensorFluxes()", getClass().getName());
            return getSensorFluxes(source);
        }
    }

    private class EspSensorFluxResolver extends SensorFluxResolver<ESPHomeListenerConfig> {

        protected EspSensorFluxResolver(List<ESPHomeListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<ESPHomeListenerConfig> source) {
            return Map.of();
        }
    }

    private class OnewireSensorFluxResolver extends SensorFluxResolver<OnewireBusConfig> {

        private OnewireSensorFluxResolver(List<OnewireBusConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<OnewireBusConfig> source) {
            return Map.of();
        }
    }
    private class ZigbeeSensorFluxResolver extends SensorFluxResolver<Z2MJsonListenerConfig> {

        private ZigbeeSensorFluxResolver(List<Z2MJsonListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<Z2MJsonListenerConfig> source) {
            return Map.of();
        }
    }

    private class ZWaveSensorFluxResolver extends SensorFluxResolver<ZWaveListenerConfig> {

        private ZWaveSensorFluxResolver(List<ZWaveListenerConfig> source) {
            super(source);
        }

        @Override
        protected Map<String, Flux<Signal<Double, Void>>> getSensorFluxes(List<ZWaveListenerConfig> source) {
            return Map.of();
        }
    }
}
