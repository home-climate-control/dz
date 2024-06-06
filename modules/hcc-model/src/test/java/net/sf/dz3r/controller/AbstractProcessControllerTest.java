package net.sf.dz3r.controller;

import net.sf.dz3r.controller.pid.SimplePidController;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractProcessControllerTest {

    @Test
    void setpointChangeEmitsSignalHysteresis() {

        Sinks.Many<Double> sink = Sinks.many().unicast().onBackpressureError();
        var source = sink
                .asFlux()
                .map(v -> new Signal<Double, Void>(Instant.now(), v));

        var pc = new HysteresisController<Void>("pc", 20);

        var accumulator = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();
        var out = pc
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        sink.tryEmitNext(15.0);
        sink.tryEmitNext(25.0);

        // This should make the controller emit a signal since the setpoint now calls for action, but
        // in imperative implementation it doesn't; it'll wait till the next incoming signal to do so
        pc.setSetpoint(30.0);

        sink.tryEmitNext(35.0);

        sink.tryEmitComplete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(4);

        // PV change
        assertThat(accumulator.get(0).getValue().signal).isEqualTo(-1.0);
        assertThat(accumulator.get(1).getValue().signal).isEqualTo(1.0);

        // Setpoint change
        assertThat(accumulator.get(2).getValue().signal).isEqualTo(-1.0);

        // PV change again
        assertThat(accumulator.get(3).getValue().signal).isEqualTo(1.0);

        out.dispose();
    }

    @Test
    void setpointChangeEmitsSignalPid() {

        Sinks.Many<Double> sink = Sinks.many().unicast().onBackpressureError();
        var source = sink
                .asFlux()
                .map(v -> new Signal<Double, Void>(Instant.now(), v));


        var pc = new SimplePidController<Void>("pc", 20d, 1, 0, 0, 1.1);

        var accumulator = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();
        var out = pc
                .compute(source)
                .log()
                .subscribe(accumulator::add);

        sink.tryEmitNext(15.0);
        sink.tryEmitNext(25.0);

        pc.setSetpoint(30.0);

        sink.tryEmitNext(35.0);

        sink.tryEmitComplete();

        // Three signals corresponding to process variable change, and one to setpoint change
        assertThat(accumulator).hasSize(4);

        // PV change
        assertThat(accumulator.get(0).getValue().signal).isEqualTo(-5.0);
        assertThat(accumulator.get(1).getValue().signal).isEqualTo(5.0);

        // Setpoint change
        assertThat(accumulator.get(2).getValue().signal).isEqualTo(-5.0);

        // PV change again
        assertThat(accumulator.get(3).getValue().signal).isEqualTo(5.0);

        out.dispose();
    }
}
