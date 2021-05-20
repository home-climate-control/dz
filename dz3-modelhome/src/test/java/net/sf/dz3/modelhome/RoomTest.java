package net.sf.dz3.modelhome;

import net.sf.dz3.device.model.HvacMode;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Test case for {@link Room room simulator}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2012
 */
class RoomTest {
    
    private final NumberFormat format = new DecimalFormat("###.###");

    @Test
    public void test() {
        
        // Play with values...
        
        double area = 20; // 215.27 sq ft
        double ceiling = 2.4384; // 8 ft
        double insulationConductivity = 10;
        double supplyQuality = 1;
        double indoorTemperature = 20; // 68°F
        double outdoorTemperature = 30; // 86°F
        long quantMillis = 1000 * 60; // 60 seconds
        long acStart = quantMillis * 30;
        long acStop = quantMillis * 90;
        double capacity = 100;
        double cooling = -capacity;
        double heatTolerance = 2;
        HvacUnit unit = new HvacUnit(HvacMode.COOLING, capacity, heatTolerance, outdoorTemperature);
        
        Room room1 = new Room(
                area, ceiling,
                insulationConductivity, supplyQuality,
                indoorTemperature, outdoorTemperature);
        
        Room room2 = new Room(
                area, ceiling,
                insulationConductivity, supplyQuality,
                indoorTemperature, outdoorTemperature);

        // ...and paste the output into the spreadsheet to visualize them.
        
        System.err.println("Time, minutes\tRoom 1 temperature, °C\tRoom 2 temperature, °C\tHVAC output");
        System.err.println("0\t" + indoorTemperature + "\t" + indoorTemperature + "\t0");
        
        for (long now = 0; now < quantMillis * 720; now += quantMillis) {
            
            double energy1 = (now >= acStart && now <= acStop) ? cooling * quantMillis / 1000 : 0;
            double energy2 = (now >= acStart && now <= acStop) ? unit.produce(room2.getInternalTemperature(), quantMillis) : 0;
            
            double t1 = room1.consume(energy1, quantMillis);
            double t2 = room2.consume(energy2, quantMillis);
            
            System.err.println(
                    ((double) (now + quantMillis) / (1000d * 60d))
                    + "\t" + format.format(t1)
                    + "\t" + format.format(t2)
                    + "\t" + energy2);
        }
    }
}
