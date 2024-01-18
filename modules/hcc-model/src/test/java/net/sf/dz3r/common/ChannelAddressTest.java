package net.sf.dz3r.common;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChannelAddressTest {

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
                .withMessage("Channel number must be non-negative (-1 given)");
    }

    /**
     * Make sure that <a href="https://errorprone.info/bugpattern/StringSplitter">this corner case</a> is covered.
     */
    @Test
    void errorProneTriggerEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IntegerChannelAddress(""))
                .withMessage("Invalid address, format: ${1-wire-address}:${channel}");
    }

    /**
     * Make sure that <a href="https://errorprone.info/bugpattern/StringSplitter">this corner case</a> is covered.
     */
    @Test
    void errorProneTriggerSeparator() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IntegerChannelAddress(":"))
                .withMessage("Invalid address, format: ${1-wire-address}:${channel}");
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
