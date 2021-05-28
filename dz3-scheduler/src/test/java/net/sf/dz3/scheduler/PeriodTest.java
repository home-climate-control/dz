package net.sf.dz3.scheduler;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PeriodTest {

    private static final long TWO_FIFTEEN = 2 * 1000 * 60 * 60 + 15 * 1000 * 60;
    private static final long FOURTEEN_FIFTEEN = 14 * 1000 * 60 * 60 + 15 * 1000 * 60;

    @Test
    void testNullName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Period(null, "02:15", "02:20", "       "))
                .withMessage("name can't be null or empty");
    }

    @Test
    void testEmptyName() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Period("", "02:15", "02:20", "       "))
                .withMessage("name can't be null or empty");
    }

    @Test
    void testNullDays() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Period("period", "02:15", "02:20", null))
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testNot7() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Period("period", "02:15", "02:20", ""))
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testTwoFifteen() {

        Period p = new Period("period", "02:15", "02:20", "       ");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);
        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x00);
        // Wrong string representation
        assertThat(p.toString()).hasToString("period (02:15 to 02:20 on .......)");
    }

    @Test
    void testFourteenFifteen() {

        Period p = new Period("period", "14:15", "14:20", "       ");

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

                    Period p = new Period("period", "0:15", "0:20", "      ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(15 * 1000 * 60);

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

                    Period p = new Period("period", "0:15", "0:20", "        ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(15 * 1000 * 60);

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("days argument malformed, see source code for instructions");
    }

    @Test
    void testDaysMWTS() {

        Period p = new Period("period", "0:15", "0:20", "M WT  S");

        // Wrong time
        assertThat(p.start).isEqualTo(15 * 1000 * 60);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x4D);
    }

    @Test
    void testDaysMTSS() {

        Period p = new Period("period", "0:15","0:20",  "MT   SS");

        // Wrong time
        assertThat(p.start).isEqualTo(15 * 1000 * 60);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x63);
    }

    @Test
    void testDaysMTWTFSS() {

        Period p = new Period("period", "0:15","0:20",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(15 * 1000 * 60);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimeAM() {

        Period p = new Period("period", "2:15 AM","02:20 AM",  ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(TWO_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimePM() {

        Period p = new Period("period", "2:15 PM", "02:20 PM", ".......");

        // Wrong time
        assertThat(p.start).isEqualTo(FOURTEEN_FIFTEEN);

        // Wrong days
        assertThat(p.days).isEqualTo((byte) 0x7F);
    }

    @Test
    void testTimeMilitary() {

        Period p = new Period("period", "1415", "1420", ".......");

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

                    Period p = new Period("period", "oops", "oops again", "       ");

                    // Wrong time
                    assertThat(p.start).isEqualTo(15 * 1000 * 60);

                    // Wrong days
                    assertThat(p.days).isEqualTo((byte) 0x00);

                })
                .withMessage("Tried all available formats ('yy-MM-dd'T'hh:mm', 'KK:mm aa', 'hh:mm aa', 'HH:mm', 'HHmm') to parse 'oops'and failed, giving up");
    }

    @Test
    void testCompareTo() {

        Period p1 = new Period("period 1", "1415", "1420", ".......");
        Period p2 = new Period("period 2", "1416", "1421", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(-1000 * 60);

        SortedSet<Period> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p1);
        assertThat(set).hasSize(2);
    }

    @Test
    void testCompareToSameStart() {

        Period p1 = new Period("period 1", "1415", "1420", ".......");
        Period p2 = new Period("period 2", "1415", "1425", ".......");
        int result = p1.compareTo(p2);

        assertThat(result).isEqualTo(300000);

        SortedSet<Period> set = new TreeSet<>();

        set.add(p2);
        set.add(p1);

        assertThat(set.first()).isEqualTo(p2);
        assertThat(set).hasSize(2);
    }

    @Test
    void testDayOffset() {

        assertThat(sunday2monday(1)).as("MO").isEqualTo(0);
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

        testIncludesDay(new DateTime().withDate(2010, 1, 18), "M      ");
        testIncludesDay(new DateTime().withDate(2010, 1, 19), " T     ");
        testIncludesDay(new DateTime().withDate(2010, 1, 20), "  W    ");
        testIncludesDay(new DateTime().withDate(2010, 1, 21), "   T   ");
        testIncludesDay(new DateTime().withDate(2010, 1, 22), "    F  ");
        testIncludesDay(new DateTime().withDate(2010, 1, 23), "     S ");
        testIncludesDay(new DateTime().withDate(2010, 1, 24), "      S");
    }

    private void testIncludesDay(DateTime d, String days) {

        Period p = new Period("period", "1415", "1420", days);

        assertThat(p.includesDay(d)).as("Wrong inclusion for " + d).isTrue();
    }

    @Test
    void testToString() {

        Period p = new Period("period", "1415", "1420", ".......");

        assertThat(p.toString()).hasToString("period (14:15 to 14:20 on MTWTFSS)");
    }
}
