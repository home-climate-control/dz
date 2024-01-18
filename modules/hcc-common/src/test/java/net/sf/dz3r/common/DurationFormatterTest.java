package net.sf.dz3r.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DurationFormatterTest {

    private final DurationFormatter f = new DurationFormatter();


    @ParameterizedTest
    @MethodSource("durationProvider")
    void format(Millis2Strings source) {
        assertThat(f.format(source.d.toMillis())).isEqualTo(source.string);
    }

    record Millis2Strings(
            Duration d,
            String string
    ) {

    }

    public static Stream<Millis2Strings> durationProvider() {

        return Stream.of(
                new Millis2Strings(Duration.ofMillis(300), "0.3s"),
                new Millis2Strings(Duration.ofSeconds(30), "30s"),
                new Millis2Strings(Duration.ofSeconds(300), "5m"),
                new Millis2Strings(Duration.ofSeconds(330), "5m 30s"),
                new Millis2Strings(Duration.ofMillis(330_010), "5m 30.01s"),
                new Millis2Strings(Duration.ofMillis(330_011), "5m 30.011s"),
                new Millis2Strings(Duration.ofMinutes(300), "5h"),

                // Less than 24 hours
                new Millis2Strings(Duration.ofMinutes(1410), "23h 30m"),
                // More than 24 hours
                new Millis2Strings(Duration.ofMinutes(1610), "1d 2h 50m"),

                new Millis2Strings(
                        Duration.ofDays(300)
                                .plus(Duration.ofMinutes(1410))
                                .plus(Duration.ofSeconds(30)),
                        "300d 23h 30m 30s"),

                new Millis2Strings(Duration.ofDays(400), "400d")

        );
    }
}
