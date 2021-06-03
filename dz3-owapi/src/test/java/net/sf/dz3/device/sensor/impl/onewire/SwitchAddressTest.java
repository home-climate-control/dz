package net.sf.dz3.device.sensor.impl.onewire;

import net.sf.dz3.device.sensor.impl.IntegerChannelAddress;
import net.sf.dz3.device.sensor.impl.StringChannelAddress;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Set of test cases for {@link StringChannelAddress}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
class SwitchAddressTest {

    @Test
    void good() {

        assertThatCode(() -> new StringChannelAddress("1300000000E6B51F:1")).doesNotThrowAnyException();
    }

    @Test
    void noChannel() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StringChannelAddress("1300000000E6B51F"))
                .withMessage("Channel not present (separator is ':', remember?)");
    }

    @Test
    void notDecimal() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IntegerChannelAddress("1300000000E6B51F:0x0f"))
                .withMessage("For input string: \"0x0f\"");
    }

    @Test
    void tooManyParts() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StringChannelAddress("1300000000E6B51F:1:2"))
                .withMessage("Too many parts (separator is ':', remember?)");
    }

    @Test
    void negativeChannel() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IntegerChannelAddress("1300000000E6B51F:-1"))
                .withMessage("Channel number is non-negative (-1 given)");
    }

    @Test
    void comparable() {

        StringChannelAddress s0 = new StringChannelAddress("1300000000E6B51F:0");
        StringChannelAddress s1 = new StringChannelAddress("1300000000E6B51F:1");

        assertThat(s1).isGreaterThan(s0);

        Set<StringChannelAddress> set = new TreeSet<>();

        set.add(s0);
        set.add(s1);

        assertThat(set).hasSize(2);
    }
}
