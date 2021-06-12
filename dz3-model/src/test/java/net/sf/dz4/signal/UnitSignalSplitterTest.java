package net.sf.dz4.signal;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.model.UnitSignal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class UnitSignalSplitterTest {

    @Test
    void split() {

        var now = Clock.systemUTC().instant().toEpochMilli();
        var ds1 = new DataSample<UnitSignal>(now, "source", "signature", new UnitSignal(0.3, true, 5), null);
        var ds2 = new DataSample<UnitSignal>(now, "source", "signature", new UnitSignal(0.4, false, 0), null);
        var ds3 = new DataSample<UnitSignal>(now, "source", "signature", null, new IllegalStateException("oops"));

        var flux = Flux.just(ds1, ds2, ds3);
        var result = new UnitSignalSplitter().split(flux);

        // VT: NOTE: The compiler won't be tricked into allowing isTrue() and isFalse(), disabling Sonar there

        StepVerifier
                .create(result)
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(true)) // NOSONAR
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(0.3))
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(5L))
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(false)) // NOSONAR
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(0.4))
                .assertNext(s -> assertThat(((DataSample) s).sample).isEqualTo(0L)) // NOSONAR
                .assertNext(s -> assertThat(((DataSample) s).error).isInstanceOf(IllegalStateException.class))
                .assertNext(s -> assertThat(((DataSample) s).error).isInstanceOf(IllegalStateException.class))
                .assertNext(s -> assertThat(((DataSample) s).error).isInstanceOf(IllegalStateException.class))
                .verifyComplete();
    }
}
