package net.sf.dz3r.runtime.config.hardware;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.counter.FileTimeUsageCounter;
import net.sf.dz3r.counter.LoggerTimeUsageReporter;
import net.sf.dz3r.counter.ResourceUsageCounter;
import net.sf.dz3r.device.actuator.HeatPump;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import net.sf.dz3r.device.actuator.VariableHvacDevice;
import net.sf.dz3r.device.actuator.pi.autohat.HeatPumpHAT;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public class HvacConfigurationParser extends ConfigurationContextAware {

    /**
     * Directory to keep the counter files in.
     */
    private final File countersRoot;

    public HvacConfigurationParser(ConfigurationContext context) {
        super(context);
        countersRoot = getCountersDirectory();
    }

    private ResourceUsageCounter<Duration> createFileCounter(String id, FilterConfig config) {

        try {

            var lifetime = Optional.ofNullable(config).map(FilterConfig::lifetime).orElse(Duration.ofHours(200));
            return new FileTimeUsageCounter(
                    id,
                    lifetime,
                    new File(countersRoot, id),
                    Set.of(
                            new LoggerTimeUsageReporter(id)
                    ));

        } catch (IOException ex) {

            logger.error("couldn't create counter for id={}, config={}, countersRoot={}", id, config, countersRoot, ex);
            return null;
        }
    }

    public void parse(Set<HvacDeviceConfig> source) {

        for (var entry : Optional.ofNullable(source).orElse(Set.of())) {
            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.heatpump())
                            .orElse(Set.of()))
                    .map(this::parseHeatpump)
                    .subscribe(h -> context.hvacDevices.register(h.getAddress(), h));

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.heatpumpHat())
                            .orElse(Set.of()))
                    .map(this::parseHeatpumpHAT)
                    .subscribe(h -> context.hvacDevices.register(h.getAddress(), h));

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.switchable())
                            .orElse(Set.of()))
                    .map(this::parseSwitchable)
                    .subscribe(h -> context.hvacDevices.register(h.getAddress(), h));

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.variable())
                            .orElse(Set.of()))
                    .map(this::parseVariable)
                    .subscribe(h -> context.hvacDevices.register(h.getAddress(), h));
        }
    }

    private File getCountersDirectory() {

        // VT: NOTE: This may need to get more complicated... but all in due time. It's $HOME/.dz/counters for now.

        var home = new File(System.getProperty("user.home"));
        return new File(home, ".dz/counters");
    }

    private HvacDevice<?> parseHeatpump(HeatpumpConfig cf) {

        return new HeatPump(
                cf.id(),
                getSwitch(cf.switchMode()),
                Optional.ofNullable(cf.switchModeReverse()).orElse(false),
                getSwitch(cf.switchRunning()),
                Optional.ofNullable(cf.switchRunningReverse()).orElse(false),
                getSwitch(cf.switchFan()),
                Optional.ofNullable(cf.switchFanReverse()).orElse(false),
                cf.modeChangeDelay(),
                createFileCounter(cf.id(), cf.filter()));
    }

    private HvacDevice<?> parseHeatpumpHAT(HeatpumpHATConfig cf) {

        try {
            return new HeatPumpHAT(
                    cf.id(),
                    createFileCounter(cf.id(), cf.filter()));
        } catch (IOException ex) {
            throw new IllegalStateException("Can't instantiate HeatPumpHAT", ex);
        }
    }

    private HvacDevice<?> parseSwitchable(SwitchableHvacDeviceConfig cf) {

        // VT: NOTE: There is no configuration keyword for the switch being inverted;
        // likely it will never be needed
        return new SwitchableHvacDevice(
                Clock.systemUTC(),
                cf.id(),
                HvacMode.valueOf(HCCObjects.requireNonNull(cf.mode(), "switchable.mode can't be null").toUpperCase()),
                getSwitch(HCCObjects.requireNonNull(cf.switchAddress(), "switchable.switch-address can't be null")),
                false,
                createFileCounter(cf.id(), cf.filter()));
    }

    private HvacDevice<?> parseVariable(VariableHvacConfig cf) {

        return new VariableHvacDevice(
                Clock.systemUTC(),
                cf.id(),
                HvacMode.valueOf(HCCObjects.requireNonNull(cf.mode(), "variable.mode can't be null").toUpperCase()),
                getFans(HCCObjects.requireNonNull(cf.actuator(), "variable.actuator can't be null")),
                Optional.ofNullable(cf.maxPower()).orElse(1d),
                Optional.ofNullable(cf.bandCount()).orElse(10),
                createFileCounter(cf.id(), cf.filter()));
    }
}
