package net.sf.dz3.util.counter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBasedUsageTest {

    @Test
    public void normalOn() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(before, 1);

        assertThat(tbu.consume(after, 1)).isEqualTo(10);
    }

    @Test
    public void normalOff() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(before, 0);

        assertThat(tbu.consume(after, 0)).isEqualTo(0);
    }

    @Test
    public void backInTime() {
        
        TimeBasedUsage tbu = new TimeBasedUsage();
        
        long before = System.currentTimeMillis();
        long after = before + 10;

        tbu.consume(after, 1);

        assertThat(tbu.consume(after, 1)).isEqualTo(0);
    }
}
