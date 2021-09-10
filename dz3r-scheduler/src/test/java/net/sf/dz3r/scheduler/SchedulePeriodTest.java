package net.sf.dz3r.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SchedulePeriodTest {

    private final Logger logger = LogManager.getLogger();

    private static final LocalTime TWO_FIFTEEN = LocalTime.parse("02:15");
    private static final LocalTime FOURTEEN_FIFTEEN = LocalTime.parse("14:15");

    @Test
    void testNullName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SchedulePeriod(null, "02:15", "02:20", "       "))
                .withMessage("name can't be null or empty");
    }

    @Test
    void testEmptyName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SchedulePeriod("", "02:15", "02:20", "       "))
                .withMessage("name can't be null or empty");
    }

    @Test
    void testNullDays() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SchedulePeriod("period", "02:15", "02:20", null))
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testNot7() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SchedulePeriod("period", "02:15", "02:20", ""))
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testTwoFifteen() {

        SchedulePeriod p = new SchedulePeriod("period", "02:15", "02:20", "       ");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);
        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x00);
        // Wrong string representation
        assertThat(p.toString()).hasToString("period (02:15 to 02:20 on .......)");
    }

    @Test
    void testFourteenFifteen() {

        SchedulePeriod p = new SchedulePeriod("period", "14:15", "14:20", "       ");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);
        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x00);
        // Wrong string representation
        assertThat(p.toString()).hasToString("period (14:15 to 14:20 on .......)");
    }

    @Test
    void testDaysTooShort() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = new SchedulePeriod("period", "0:15", "0:20", "      ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testDaysTooLong() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = new SchedulePeriod("period", "0:15", "0:20", "        ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testDaysMWTS() {

        SchedulePeriod p = new SchedulePeriod("period", "0:15", "0:20", "M WT  S");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x4D);
    }

    @Test
    void testDaysMTSS() {

        SchedulePeriod p = new SchedulePeriod("period", "0:15","0:20",  "MT   SS");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x63);
    }

    @Test
    void testDaysMTWTFSS() {

        SchedulePeriod p = new SchedulePeriod("period", "0:15","0:20",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimeAM() {

        SchedulePeriod p = new SchedulePeriod("period", "2:15 AM","02:20 AM",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimePM() {

        SchedulePeriod p = new SchedulePeriod("period", "2:15 PM", "02:20 PM", ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimeMilitary() {

        SchedulePeriod p = new SchedulePeriod("period", "1415", "1420", ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testBadTime() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = new SchedulePeriod("period", "oops", "oops again", "       ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("Tried all available formats (see the log for details) and failed, giving up");
    }

    @Test
    void testCompareTo() {

        SchedulePeriod p1 = new SchedulePeriod("period 1", "1415", "1420", ".......");
        SchedulePeriod p2 = new SchedulePeriod("period 2", "1416", "1421", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(-1);

        SortedSet<SchedulePeriod> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p1);
        assertThat(set).hasSize(2);
    }

    @Test
    void testCompareToSameStart() {

        SchedulePeriod p1 = new SchedulePeriod("period 1", "1415", "1420", ".......");
        SchedulePeriod p2 = new SchedulePeriod("period 2", "1415", "1425", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(1);

        SortedSet<SchedulePeriod> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p2);
        assertThat(set).hasSize(2);
    }

    @Test
    void testDayOffset() {

        assertThat(sunday2monday(1)).as("MO").isZero();
        assertThat(sunday2monday(2)).as("TU").isEqualTo(1);
        assertThat(sunday2monday(3)).as("WE").isEqualTo(2);
        assertThat(sunday2monday(4)).as("TH").isEqualTo(3);
        assertThat(sunday2monday(5)).as("FR").isEqualTo(4);
        assertThat(sunday2monday(6)).as("SA").isEqualTo(5);
        assertThat(sunday2monday(0)).as("SU").isEqualTo(6);
    }

    private int sunday2monday(int sunday) {

        return (sunday + 6) % 7;
    }

    @Test
    void testIncludesDayMo() {

        testIncludesDay(LocalDate.parse("2010-01-18"), "M      ");
        testIncludesDay(LocalDate.parse("2010-01-19"), " T     ");
        testIncludesDay(LocalDate.parse("2010-01-20"), "  W    ");
        testIncludesDay(LocalDate.parse("2010-01-21"), "   T   ");
        testIncludesDay(LocalDate.parse("2010-01-22"), "    F  ");
        testIncludesDay(LocalDate.parse("2010-01-23"), "     S ");
        testIncludesDay(LocalDate.parse("2010-01-24"), "      S");
    }

    private void testIncludesDay(LocalDate d, String days) {

        SchedulePeriod p = new SchedulePeriod("period", "1415", "1420", days);

        assertThat(p.includesDay(d)).as("Wrong inclusion for " + d).isTrue();
    }

    @Test
    void testToString() {

        SchedulePeriod p = new SchedulePeriod("period", "1415", "1420", ".......");

        assertThat(p.toString()).hasToString("period (14:15 to 14:20 on MTWTFSS)");
    }
}
