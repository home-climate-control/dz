package net.sf.dz3.controller.pid;

import net.sf.dz3.controller.LegacyDataSet;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Data set supporting the integration calculation.
 * <p>
 * The {@link DataSet#append()} method from {@link DataSet DataSet} class
 * is used, however, make sure you record the right values. If this class is
 * used for the {@link PID_Controller}, it must be fed with controller error,
 * and anti-windup action must be programmed outside of this class.
 *
 * This is the old implementation, written in 2000 with little regard to performance.
 *
 * @see IntegralSet
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-20012
 */
public class LegacyIntegralSet extends LegacyDataSet<Double> implements IntegralSet {

    /**
     * Create the instance.
     *
     * @param integrationTime Integration time, milliseconds. Data elements older than this are expired.
     */
    public LegacyIntegralSet(final long integrationTime) {

        super(integrationTime);
    }

  /**
   * Get the integral starting with the first data element available and
   * ending with the last data element available.
   * <p>
   * Integration time must have been taken care of by {@link DataSet#expire
   * expiration}.
   *
   * @return An integral value.
   */
    @Override
    public final synchronized double getIntegral() {

        double result = 0;

        //long startTime = System.currentTimeMillis();

        try {

            // The following line will throw the NoSuchElementException if
            // the dataSet is empty. It is possible to check the size, but
            // handling the exception is more efficient because happens just
            // once - at the startup, or when there was an interruption in
            // the data sequence longer than the expiration interval and all
            // the data elements were expired.

            Iterator<Map.Entry<Long, Double>> i = entryIterator();
            Map.Entry<Long, Double> trailerEntry = i.next();

            while (i.hasNext()) {

                long trailerKey = trailerEntry.getKey();
                double trailerValue = trailerEntry.getValue();

                Map.Entry<Long, Double> currentEntry = i.next();

                long currentKey = currentEntry.getKey();
                double currentValue = currentEntry.getValue();

                double diff = ((currentValue + trailerValue) / 2) * (currentKey - trailerKey);

                result += diff;

                trailerEntry = currentEntry;
            }

            return result;

        } catch (NoSuchElementException nseex) {

            System.err.println("Ignored:");
            nseex.printStackTrace();

            return result;

        } finally {

            // System.err.println("getIntegral@" + hashCode() + "," +
            // dataSet.size() + ": " + (System.currentTimeMillis() -
            // startTime));
        }
    }
}
