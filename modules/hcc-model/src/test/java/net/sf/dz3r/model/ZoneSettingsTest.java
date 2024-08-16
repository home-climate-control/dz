package net.sf.dz3r.model;

import net.sf.dz3r.device.actuator.economizer.EconomizerSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneSettingsTest {

    @ParameterizedTest
    @MethodSource("settingsProvider")
    void same(Source source) {
        assertThat(source.a.same(source.b)).isEqualTo(source.same);
    }

    private record Source(
            ZoneSettings a,
            ZoneSettings b,
            boolean same
    ) {

    }

    private static Stream<Source> settingsProvider() {

        return Stream.of(
                new Source(
                        new ZoneSettings(25.0),
                        new ZoneSettings(25.0),
                        true
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                null),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                null),
                        true
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                null),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                new EconomizerSettings(
                                        2,
                                        22,
                                        null,
                                        null
                                )
                        ),
                        false
                ),
                new Source(
                        new ZoneSettings(
                                true,
                                25d,
                                true,
                                false,
                                0,
                                new EconomizerSettings(
                                        2d,
                                        22d,
                                        null,
                                        null
                                )
                        ),
                        new ZoneSettings(
                                null,
                                25d,
                                null,
                                null,
                                null,
                                new EconomizerSettings(
                                        2d,
                                        22d,
                                        true,
                                        1d
                                )
                        ),
                        true
                )
        );
    }
}
