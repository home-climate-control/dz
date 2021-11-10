package net.sf.dz3r.controller;

import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractProcessControllerTest {

    private FluxSink<Double> pvSink;

    @Test
    void setpointChangeEmitsSignalHysteresis() {

        var source = Flux
                .create(this::connectSetpoint)
                .map(v -> new Signal<Double, Void>(Instant.now(), v));

        var pc = new HysteresisController<Void>("pc", 20);

        var accumulator = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();
        var out = pc
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        pvSink.next(15.0);
        pvSink.next(25.0);

        // This should make the controller emit a signal since the setpoint now calls for action, but
        // in imperative implementation it doesn't; it'll wait till the next incoming signal to do so
        pc.setSetpoint(30.0);

        pvSink.next(25.0);

        pvSink.complete();

        // This is also wrong, should be 4
        assertThat(accumulator).hasSize(3);

        // This is right
        assertThat(accumulator.get(0).getValue().signal).isEqualTo(-1.0);
        assertThat(accumulator.get(1).getValue().signal).isEqualTo(1.0);

        // And this is wrong, one signal is missing
        assertThat(accumulator.get(2).getValue().signal).isEqualTo(-1.0);

        out.dispose();
    }

    @Test
    void setpointChangeEmitsSignalPid() {

        var source = Flux
                .create(this::connectSetpoint)
                .map(v -> new Signal<Double, Void>(Instant.now(), v));

        var pc = new SimplePidController<Void>("pc", 20, 1, 0, 0, 1.1);

        var accumulator = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();
        var out = pc
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        pvSink.next(15.0);
        pvSink.next(25.0);

        // This should make the controller emit a signal since the setpoint now calls for action, but
        // in imperative implementation it doesn't; it'll wait till the next incoming signal to do so
        pc.setSetpoint(30.0);

        pvSink.next(25.0);

        pvSink.complete();

        // This is also wrong, should be 4
        assertThat(accumulator).hasSize(3);

        // This is right
        assertThat(accumulator.get(0).getValue().signal).isEqualTo(-5.0);
        assertThat(accumulator.get(1).getValue().signal).isEqualTo(5.0);

        // And this is wrong, one signal is missing
        assertThat(accumulator.get(2).getValue().signal).isEqualTo(-5.0);

        out.dispose();
    }

    private void connectSetpoint(FluxSink<Double> pvSink) {
        this.pvSink = pvSink;
    }
}
