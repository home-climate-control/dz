package net.sf.dz3.view.swing.unit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class UnitPanelTest {

    @Test
    void formatTime() {
        assertThat(UnitPanel.format(Duration.of(59, ChronoUnit.SECONDS), false)).isEqualTo("<1 min");
        assertThat(UnitPanel.format(Duration.of(59, ChronoUnit.SECONDS).plus(Duration.of(1, ChronoUnit.HOURS)), false)).isEqualTo("1 hr");
        assertThat(UnitPanel.format(Duration.of(42, ChronoUnit.MINUTES), false)).isEqualTo("42 min");
        assertThat(UnitPanel.format(Duration.of(59, ChronoUnit.MINUTES).plus(Duration.of(1, ChronoUnit.HOURS)), false)).isEqualTo("1 hr 59 min");
    }
}
