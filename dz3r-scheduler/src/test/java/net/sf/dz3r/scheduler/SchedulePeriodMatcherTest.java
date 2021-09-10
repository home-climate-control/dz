package net.sf.dz3r.scheduler;

/**
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2018
 */
class SchedulePeriodMatcherTest {

//    @Test
//    void testNone() {
//
//        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
//        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0).withMinuteOfHour(40);
//
//        zoneSchedule.put(new SchedulePeriod("period", "0:15", "0:30", "......."), null);
//
//        assertThatExceptionOfType(NoSuchElementException.class)
//                .isThrownBy(() -> test(zoneSchedule, dt));
//    }
//
//    @Test
//    void testSimple() {
//
//        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
//
//        SchedulePeriod p1 = new SchedulePeriod("period", "00:15", "00:30", ".......");
//        zoneSchedule.put(p1, null);
//
//        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
//
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(20))).isEqualTo(p1);
//    }
//
//    @Test
//    void testSimple2() {
//
//        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
//
//        // Let's make sure that hours in SchedulePeriod.includes(long) are also properly converted
//        SchedulePeriod p1 = new SchedulePeriod("period", "02:15", "02:30", ".......");
//        zoneSchedule.put(p1, null);
//
//        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(2);
//
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(20))).isEqualTo(p1);
//    }
//
//    @Test
//    void testLadder() {
//
//        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
//
//        SchedulePeriod p1 = new SchedulePeriod("period 1", "00:10", "00:30", ".......");
//        SchedulePeriod p2 = new SchedulePeriod("period 2", "00:20", "00:40", ".......");
//        SchedulePeriod p3 = new SchedulePeriod("period 3", "00:30", "00:50", ".......");
//
//        zoneSchedule.put(p1, null);
//        zoneSchedule.put(p2, null);
//        zoneSchedule.put(p3, null);
//
//        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
//
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(15))).isEqualTo(p1);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(25))).isEqualTo(p2);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(35))).isEqualTo(p3);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(45))).isEqualTo(p3);
//    }
//
//    @Test
//    void testStack() {
//
//        SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule = new TreeMap<>();
//
//        SchedulePeriod p1 = new SchedulePeriod("period 1", "00:10", "00:50", ".......");
//        SchedulePeriod p2 = new SchedulePeriod("period 2", "00:15", "00:40", ".......");
//        SchedulePeriod p3 = new SchedulePeriod("period 3", "00:20", "00:30", ".......");
//        SchedulePeriod p4 = new SchedulePeriod("period 4", "01:00", "02:00", "       ");
//
//        zoneSchedule.put(p1, null);
//        zoneSchedule.put(p2, null);
//        zoneSchedule.put(p3, null);
//        zoneSchedule.put(p4, null);
//
//        DateTime dt = new DateTime().withDate(2010, 1, 19).withHourOfDay(0);
//
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(12))).isEqualTo(p1);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(18))).isEqualTo(p2);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(22))).isEqualTo(p3);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(32))).isEqualTo(p2);
//        assertThat(test(zoneSchedule, dt.withMinuteOfHour(42))).isEqualTo(p1);
//    }
//
//    private SchedulePeriod test(SortedMap<SchedulePeriod, ZoneSettings> zoneSchedule, DateTime time) {
//        return new SchedulePeriodMatcher().match(zoneSchedule, time);
//    }
}
