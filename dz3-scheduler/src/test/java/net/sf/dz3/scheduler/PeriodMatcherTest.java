package net.sf.dz3.scheduler;

import net.sf.dz3.device.model.ZoneStatus;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.EmptyStackException;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
class PeriodMatcherTest {

    @Test
    void testNone() {

        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<>();
        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0).withMinuteOfHour(40);

        zoneSchedule.put(new Period("period", "0:15", "0:30", "......."), null);

        assertThatExceptionOfType(EmptyStackException.class)
                .isThrownBy(() -> test(zoneSchedule, dt));
    }

    @Test
    void testSimple() {

        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<>();

        Period p1 = new Period("period", "00:15", "00:30", ".......");
        zoneSchedule.put(p1, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);

        assertThat(test(zoneSchedule, dt.withMinuteOfHour(20))).isEqualTo(p1);
    }

    @Test
    void testSimple2() {

        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<>();

        // Let's make sure that hours in Period.includes(long) are also properly converted
        Period p1 = new Period("period", "02:15", "02:30", ".......");
        zoneSchedule.put(p1, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(2);

        assertThat(test(zoneSchedule, dt.withMinuteOfHour(20))).isEqualTo(p1);
    }

    @Test
    void testLadder() {

        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<>();

        Period p1 = new Period("period 1", "00:10", "00:30", ".......");
        Period p2 = new Period("period 2", "00:20", "00:40", ".......");
        Period p3 = new Period("period 3", "00:30", "00:50", ".......");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);

        assertThat(test(zoneSchedule, dt.withMinuteOfHour(15))).isEqualTo(p1);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(25))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(35))).isEqualTo(p3);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(45))).isEqualTo(p3);
    }

    @Test
    void testStack() {

        SortedMap<Period, ZoneStatus> zoneSchedule = new TreeMap<>();

        Period p1 = new Period("period 1", "00:10", "00:50", ".......");
        Period p2 = new Period("period 2", "00:15", "00:40", ".......");
        Period p3 = new Period("period 3", "00:20", "00:30", ".......");
        Period p4 = new Period("period 4", "01:00", "02:00", "       ");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);
        zoneSchedule.put(p4, null);

        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);

        assertThat(test(zoneSchedule, dt.withMinuteOfHour(12))).isEqualTo(p1);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(18))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(22))).isEqualTo(p3);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(32))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.withMinuteOfHour(42))).isEqualTo(p1);
    }

    private Period test(SortedMap<Period, ZoneStatus> zoneSchedule, DateTime time) {
        return new PeriodMatcher().match(zoneSchedule, time);
    }
}
