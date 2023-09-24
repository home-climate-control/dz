package net.sf.dz3r.scheduler;

import net.sf.dz3r.model.SchedulePeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SchedulePeriodTest {

    private final SchedulePeriodFactory schedulePeriodFactory = new SchedulePeriodFactory();

    private static final LocalTime TWO_FIFTEEN = LocalTime.parse("02:15");
    private static final LocalTime FOURTEEN_FIFTEEN = LocalTime.parse("14:15");

    @Test
    void nullName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> schedulePeriodFactory.build(null, "02:15", "02:20", "       "))
                .withMessage("Name can't be null or empty");
    }

    @Test
    void emptyName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> schedulePeriodFactory.build("", "02:15", "02:20", "       "))
                .withMessage("Name can't be null or empty");
    }

    @Test
    void nullDays() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> schedulePeriodFactory.build("period", "02:15", "02:20", null))
                .withMessage("Days argument malformed, see source code for instructions");
    }

    @Test
    void zeroLength() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> schedulePeriodFactory.build("period", "02:15", "02:15", null))
                .withMessage("Start and end time are the same: 02:15");
    }

    @Test
    void not7() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> schedulePeriodFactory.build("period", "02:15", "02:20", ""))
                .withMessage("Days argument malformed, see source code for instructions");
    }

    @Test
    void twoFifteen() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "02:15", "02:20", "       ");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);
        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x00);
        // Wrong string representation
        assertThat(p.toString()).hasToString("period (id=" + p.id + " 02:15 to 02:20 on .......)");
    }

    @Test
    void fourteenFifteen() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "14:15", "14:20", "       ");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);
        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x00);
        // Wrong string representation
        assertThat(p.toString()).hasToString("period (id=" + p.id + " 14:15 to 14:20 on .......)");
    }

    @Test
    void daysTooShort() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = schedulePeriodFactory.build("period", "0:15", "0:20", "      ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("Days argument malformed, see source code for instructions");
    }

    @Test
    void daysTooLong() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = schedulePeriodFactory.build("period", "0:15", "0:20", "        ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("Days argument malformed, see source code for instructions");
    }

    @Test
    void daysMWTS() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "0:15", "0:20", "M WT  S");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x4D);
    }

    @Test
    void daysMTSS() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "0:15","0:20",  "MT   SS");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x63);
    }

    @Test
    void daysMTWTFSS() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "0:15","0:20",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void timeAM() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "2:15 AM","02:20 AM",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void timePM() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "2:15 PM", "02:20 PM", ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void timeMilitary() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "1415", "1420", ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void badTime() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {

                    // VT: FIXME: This looks odd; did it work before?

                    SchedulePeriod p = schedulePeriodFactory.build("period", "oops", "oops again", "       ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(LocalTime.parse("00:15"));

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("Tried all available formats to parse 'oops' (see the log for details) and failed, giving up");
    }

    @Test
    void compareTo() {

        SchedulePeriod p1 = schedulePeriodFactory.build("period 1", "1415", "1420", ".......");
        SchedulePeriod p2 = schedulePeriodFactory.build("period 2", "1416", "1421", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(-1);

        SortedSet<SchedulePeriod> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p1);
        assertThat(set).hasSize(2);
    }

    @Test
    void compareToSameStart() {

        SchedulePeriod p1 = schedulePeriodFactory.build("period 1", "1415", "1420", ".......");
        SchedulePeriod p2 = schedulePeriodFactory.build("period 2", "1415", "1425", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(1);

        SortedSet<SchedulePeriod> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p2);
        assertThat(set).hasSize(2);
    }

    @Test
    void dayOffset() {

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
    void includesTime() {

        // Same day

        includesTime("02:15", "02:40", "02:20", true);
        includesTime("02:15", "02:40", "02:15", true); // beginning inclusive
        includesTime("02:20", "02:30", "02:30", false); // end exclusive
        includesTime("02:20", "02:30", "02:40", false);

        // Across midnight

        includesTime("22:15", "02:30", "02:20", true);
        includesTime("22:15", "02:30", "22:15", true); // beginning inclusive
        includesTime("22:15", "02:30", "02:30", false); // end exclusive
        includesTime("22:20", "02:30", "22:00", false);
        includesTime("22:20", "02:30", "02:40", false);
    }

    private void includesTime(String start, String end, String match, boolean expected) {
        SchedulePeriod p = schedulePeriodFactory.build("period", LocalTime.parse(start).toString(), LocalTime.parse(end).toString(), ".......");
        assertThat(p.includes(LocalTime.parse(match))).isEqualTo(expected);
    }

    @Test
    void includesDayMo() {

        includesDay(LocalDate.parse("2010-01-18"), "M      ");
        includesDay(LocalDate.parse("2010-01-19"), " T     ");
        includesDay(LocalDate.parse("2010-01-20"), "  W    ");
        includesDay(LocalDate.parse("2010-01-21"), "   T   ");
        includesDay(LocalDate.parse("2010-01-22"), "    F  ");
        includesDay(LocalDate.parse("2010-01-23"), "     S ");
        includesDay(LocalDate.parse("2010-01-24"), "      S");
    }

    private void includesDay(LocalDate d, String days) {

        SchedulePeriod p = schedulePeriodFactory.build("period", "1415", "1420", days);

        assertThat(p.includesDay(d)).as("Wrong inclusion for " + d).isTrue();
    }

    @Test
    void testToString() {

        SchedulePeriod p = schedulePeriodFactory.build("period", "1415", "1420", ".......");

        assertThat(p.toString()).hasToString("period (id=" + p.id + " 14:15 to 14:20 on MTWTFSS)");
    }
}
