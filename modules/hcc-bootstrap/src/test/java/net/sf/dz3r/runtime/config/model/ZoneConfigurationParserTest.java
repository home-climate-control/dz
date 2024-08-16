package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.runtime.config.ConfigurationContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ZoneConfigurationParserTest {

    private final Random rg = new Random();
    private final Parser p = new Parser(new ConfigurationContext());

    /**
     * Make sure a sensible sensitivity configuration is returned when the value is missing from the configuration.
     */
    @Test
    void sensitivityMissing() {

        var result = p.parseSensitivity("name", null);

        assertThat(result).isEqualTo(new HalfLifeConfig(Duration.ofSeconds(10), 1d));
    }

    /**
     * Make sure a sensible sensitivity configuration is returned when the half-life value is missing from the configuration.
     */
    @Test
    void sensitivityHalfLifeMissing() {

        var multiplier = Math.abs(rg.nextDouble());
        var source = new HalfLifeConfig(null, multiplier);
        var result = p.parseSensitivity("name", source);

        assertThat(result).isEqualTo(new HalfLifeConfig(Duration.ofSeconds(10), multiplier));
    }

    /**
     * Make sure a sensible sensitivity configuration is returned when the multiplier value is missing from the configuration.
     */
    @Test
    void sensitivityMultiplierMissing() {

        var halfLife = Duration.ofSeconds(Math.abs(rg.nextInt()));
        var source = new HalfLifeConfig(halfLife, null);
        var result = p.parseSensitivity("name", source);

        assertThat(result).isEqualTo(new HalfLifeConfig(halfLife, 1d));
    }

    /**
     * Make sure negative half-life is rejected.
     */
    @Test
    void sensitivityHalfLifeNegative() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> p.parseSensitivity("name", new HalfLifeConfig(Duration.ofSeconds(-1), null)));
    }

    /**
     * Make sure negative multiplier is rejected.
     */
    @Test
    void sensitivityMultiplierNegative() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> p.parseSensitivity("name", new HalfLifeConfig(null, -1d)));
    }

    private static class Parser extends ZoneConfigurationParser {

        public Parser(ConfigurationContext context) {
            super(context);
        }
    }
}
