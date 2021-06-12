package net.sf.dz4.signal;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.HvacSignal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class HvacSignalSplitterTest {

    @Test
    void split() {

        var now = Clock.systemUTC().instant().toEpochMilli();
        var ds1 = new DataSample<HvacSignal>(now, "source", "signature", new HvacSignal(HvacMode.HEATING, 0.3, true, 5), null);
        var ds2 = new DataSample<HvacSignal>(now, "source", "signature", new HvacSignal(HvacMode.HEATING, 0.4, false, 0), null);
        var ds3 = new DataSample<HvacSignal>(now, "source", "signature", null, new IllegalStateException("oops"));

        var flux = Flux.just(ds1, ds2, ds3);
        var result = new HvacSignalSplitter().split(flux);

        // VT: NOTE: The compiler won't be tricked into allowing isTrue() and isFalse(), disabling Sonar there

        StepVerifier
                .create(result)
                .assertNext(s -> assertThat(s.sample).isEqualTo(HvacMode.HEATING))
                .assertNext(s -> assertThat(s.sample).isEqualTo(true)) // NOSONAR
                .assertNext(s -> assertThat(s.sample).isEqualTo(0.3))
                .assertNext(s -> assertThat(s.sample).isEqualTo(5L))
                .assertNext(s -> assertThat(s.sample).isEqualTo(HvacMode.HEATING))
                .assertNext(s -> assertThat(s.sample).isEqualTo(false)) // NOSONAR
                .assertNext(s -> assertThat(s.sample).isEqualTo(0.4))
                .assertNext(s -> assertThat(s.sample).isEqualTo(0L)) // NOSONAR
                .assertNext(s -> assertThat(s.error).isInstanceOf(IllegalStateException.class))
                .assertNext(s -> assertThat(s.error).isInstanceOf(IllegalStateException.class))
                .assertNext(s -> assertThat(s.error).isInstanceOf(IllegalStateException.class))
                .assertNext(s -> assertThat(s.error).isInstanceOf(IllegalStateException.class))
                .verifyComplete();
    }
}
