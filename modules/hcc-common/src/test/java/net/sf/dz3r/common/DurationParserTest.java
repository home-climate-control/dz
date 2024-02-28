package net.sf.dz3r.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DurationParserTest {

    private final DurationParser dp = new DurationParser();

    @ParameterizedTest
    @MethodSource("durationProvider")
    void parse(String2Duration source) {

        var actual = dp.parse(source.s);

        assertThat(actual).isEqualTo(source.d);
    }

    private static Stream<String2Duration> durationProvider() {
        return Stream.of(
                new String2Duration("PT200H", Duration.ofHours(200)),
                new String2Duration("PT200", Duration.ofHours(0)),
                new String2Duration("200H", Duration.ofHours(200)),
                new String2Duration("200M", Duration.ofMinutes(200)),
                new String2Duration("200S", Duration.ofSeconds(200)),
                new String2Duration("40H3M4S", Duration.parse("PT40H3M4S")),
                new String2Duration("200", Duration.ofHours(200)),
                new String2Duration("0", Duration.ofHours(0)),
                new String2Duration("huh?", Duration.ofHours(0))
        );
    }

    private record String2Duration(
            String s,
            Duration d
    ) {}
}
