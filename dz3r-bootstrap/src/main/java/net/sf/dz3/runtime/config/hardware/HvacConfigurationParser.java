package net.sf.dz3.runtime.config.hardware;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.device.actuator.HeatPump;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import net.sf.dz3r.device.actuator.pi.autohat.HeatPumpHAT;
import net.sf.dz3r.model.HvacMode;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class HvacConfigurationParser extends ConfigurationContextAware {

    public HvacConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<HvacDeviceConfig> source) {

        for (var entry : source) {
            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.heatpump())
                            .orElse(Set.of()))
                    .map(this::parseHeatpump)
                    .subscribe(context::registerHVAC);

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.heatpumpHat())
                            .orElse(Set.of()))
                    .map(this::parseHeatpumpHAT)
                    .subscribe(context::registerHVAC);

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.switchable())
                            .orElse(Set.of()))
                    .map(this::parseSwitchable)
                    .subscribe(context::registerHVAC);
        }
    }

    private HvacDevice parseHeatpump(HeatpumpConfig cf) {

        // VT: FIXME: Need to have the switches available here, but they haven't been exposed yet

        return new HeatPump(
                cf.id(),
                new NullSwitch(cf.id() + "mode"),
                Optional.ofNullable(cf.switchModeReverse()).orElse(false),
                new NullSwitch(cf.id() + "running"),
                Optional.ofNullable(cf.switchRunningReverse()).orElse(false),
                new NullSwitch(cf.id() + "fan"),
                Optional.ofNullable(cf.switchFanReverse()).orElse(false),
                cf.modeChangeDelay());
    }

    private HvacDevice parseHeatpumpHAT(HeatpumpHATConfig cf) {

        try {
            return new HeatPumpHAT(cf.id());
        } catch (IOException ex) {
            throw new IllegalStateException("Can't instantiate HeatPumpHAT", ex);
        }
    }

    private HvacDevice parseSwitchable(SwitchableHvacDeviceConfig cf) {

        // VT: FIXME: Need to have the switches available here, but they haven't been exposed yet

        return new SwitchableHvacDevice(
                cf.id(),
                HvacMode.valueOf(cf.mode().toUpperCase()),
                new NullSwitch(cf.switchAddress()));
    }
}
