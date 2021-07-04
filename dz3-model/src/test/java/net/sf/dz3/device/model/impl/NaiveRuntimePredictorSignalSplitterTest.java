package net.sf.dz3.device.model.impl;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatCode;

class NaiveRuntimePredictorSignalSplitterTest {

    @Test
    void unknown() {

        assertThatCode(() -> {

            var unknown = new DataSample<>(
                    Clock.systemUTC().instant().toEpochMilli(),
                    "source",
                    "signature",
                    new UnitRuntimePredictionSignal(
                            new HvacSignal(HvacMode.COOLING, 0, false, 0),
                            0, null, null, false),
                    null);

            new NaiveRuntimePredictorSignalSplitter().consume(unknown);

        }).doesNotThrowAnyException();
    }
}
