package net.sf.dz3r.scheduler;

import net.sf.dz3r.model.ZoneSettings;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
class SchedulePeriodMatcherTest {

    @Test
    void none() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
        LocalDateTime dt = LocalDateTime.parse("2010-01-19T00:40");

        zoneSchedule.put(new SchedulePeriod("period", "0:15", "0:30", "......."), null);

        assertThat(test(zoneSchedule, dt)).isNull();
    }

    @Test
    void simple() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();

        SchedulePeriod p1 = new SchedulePeriod("period", "00:15", "00:30", ".......");
        zoneSchedule.put(p1, null);

        LocalDateTime dt = LocalDateTime.parse("2010-01-19T00:00");

        assertThat(test(zoneSchedule, dt.plus(20, ChronoUnit.MINUTES))).isEqualTo(p1);
    }

    @Test
    void simple2() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();

        // Let's make sure that hours in SchedulePeriod.includes(long) are also properly converted
        SchedulePeriod p1 = new SchedulePeriod("period", "02:15", "02:30", ".......");
        zoneSchedule.put(p1, null);

        LocalDateTime dt = LocalDateTime.parse("2010-01-19T02:00");

        assertThat(test(zoneSchedule, dt.plus(20, ChronoUnit.MINUTES))).isEqualTo(p1);
    }

    @Test
    void ladder() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();

        SchedulePeriod p1 = new SchedulePeriod("period 1", "00:10", "00:30", ".......");
        SchedulePeriod p2 = new SchedulePeriod("period 2", "00:20", "00:40", ".......");
        SchedulePeriod p3 = new SchedulePeriod("period 3", "00:30", "00:50", ".......");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);

        LocalDateTime dt = LocalDateTime.parse("2010-01-19T00:00");

        assertThat(test(zoneSchedule, dt.plus(15, ChronoUnit.MINUTES))).isEqualTo(p1);
        assertThat(test(zoneSchedule, dt.plus(25, ChronoUnit.MINUTES))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.plus(35, ChronoUnit.MINUTES))).isEqualTo(p3);
        assertThat(test(zoneSchedule, dt.plus(45, ChronoUnit.MINUTES))).isEqualTo(p3);
    }

    @Test
    void stack() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();

        SchedulePeriod p1 = new SchedulePeriod("period 1", "00:10", "00:50", ".......");
        SchedulePeriod p2 = new SchedulePeriod("period 2", "00:15", "00:40", ".......");
        SchedulePeriod p3 = new SchedulePeriod("period 3", "00:20", "00:30", ".......");
        SchedulePeriod p4 = new SchedulePeriod("period 4", "01:00", "02:00", "       ");

        zoneSchedule.put(p1, null);
        zoneSchedule.put(p2, null);
        zoneSchedule.put(p3, null);
        zoneSchedule.put(p4, null);

        LocalDateTime dt = LocalDateTime.parse("2010-01-19T00:00");

        assertThat(test(zoneSchedule, dt.plus(12, ChronoUnit.MINUTES))).isEqualTo(p1);
        assertThat(test(zoneSchedule, dt.plus(18, ChronoUnit.MINUTES))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.plus(22, ChronoUnit.MINUTES))).isEqualTo(p3);
        assertThat(test(zoneSchedule, dt.plus(32, ChronoUnit.MINUTES))).isEqualTo(p2);
        assertThat(test(zoneSchedule, dt.plus(42, ChronoUnit.MINUTES))).isEqualTo(p1);
    }

    @Test
    void acrossMidnight() {

        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
        SchedulePeriod p1 = new SchedulePeriod("period 1", "22:00", "02:00", ".......");

        zoneSchedule.put(p1, null);

        LocalDateTime before = LocalDateTime.parse("2010-01-19T21:00"); // no match
        LocalDateTime inside = LocalDateTime.parse("2010-01-19T23:00"); // match
        LocalDateTime after = LocalDateTime.parse("2010-01-20T03:00"); // no match


        assertThat(test(zoneSchedule, before)).isNull();
        assertThat(test(zoneSchedule, inside)).isEqualTo(p1);
        assertThat(test(zoneSchedule, after)).isNull();
    }

    private SchedulePeriod test(SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule, LocalDateTime time) {
        return new SchedulePeriodMatcher().match(zoneSchedule, time);
    }
}
