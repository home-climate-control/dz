package net.sf.dz3.controller.pid;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.dz3.controller.DataSet;

/**
 * Data set supporting the differential calculation.
 *
 * This is the old implementation, written in 2000 with little regard to performance, and updated to use a better data structure in 2015.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2015
 */
public class NaiveDifferentialSet extends DataSet<Double> implements DifferentialSet {

    /**
     * Create the instance.
     *
     * @param differentialTime Differential time, milliseconds. Data elements
     * older than this are expired.
     */
    public NaiveDifferentialSet(final long differentialTime) {

        super(differentialTime, true);
    }

  /**
   * Get the differential starting with the first data element available and
   * ending with the last data element available.
   * <p>
   * Differentiation time must have been taken care of by {@link
   * DataSet#expire expiration}.
   *
   * @return A differential value.
   */
  @Override
  public final synchronized double getDifferential() {

      if (size() < 2) {

          // Nothing to compare yet

          return 0;
      }

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

          LinkedHashMap<Long, Double> resultSet = new LinkedHashMap<Long, Double>();

          while (i.hasNext()) {

              long trailerKey = trailerEntry.getKey();
              double trailerValue = trailerEntry.getValue();

              Map.Entry<Long, Double> currentEntry = i.next();

              long currentKey = currentEntry.getKey();
              double currentValue = currentEntry.getValue();

              long start = trailerKey;
              long stop = currentKey;

              double diff = (currentValue - trailerValue) / (currentKey - trailerKey);

              resultSet.put(Long.valueOf(start + ((start + stop) / 2)), Double.valueOf(diff));

              trailerEntry = currentEntry;
          }

          double result = 0;

          i = resultSet.entrySet().iterator();
          trailerEntry = i.next();

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

      } finally {

          // System.err.println("getDifferential@" + hashCode() + "," +
          // dataSet.size() + ": " + (System.currentTimeMillis() -
          // startTime));
      }
  }
}
