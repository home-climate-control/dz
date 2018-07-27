package net.sf.dz3.view.swing.thermostat;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import junit.framework.TestCase;

public class ChartTest extends TestCase {
	
	private final Logger logger = LogManager.getLogger(getClass());
	private final static Random rg = new Random();
	
	private final static Color seed = new Color(rg.nextInt(0xFFFFFF));
	
	// VT: NOTE: Uncomment this block and comment the other for more realistic comparison
	
//	private final static int COUNT = 5000000;
//	private final static int LOOP_COUNT = 10;
	
	private final static int COUNT = 50000;
	private final static int LOOP_COUNT = 5;

	/**
	 * Compare straight and cached implementations for RGB2HSB.
	 * 
	 * Reason for such complication is that even running the same method several times
	 * in a row on an otherwise idle computer produces vastly different results.
	 * Only a solid advantage (no less than 2+ times) will give a faint hope that one algorithm
	 * is indeed faster than the other. Which is the case.
	 */
	public void testAll() {
		
		long straightTime = 0;
		long cachedTime = 0;
		
		int straightCount = 0;
		int cachedCount = 0;
		
		for (int count = 0; count < LOOP_COUNT; count++) {
			
			boolean straightFirst = rg.nextBoolean();
			
			if (straightFirst) {

				straightTime += straight();
				straightCount++;
				
				cachedTime += cached();
				cachedCount++;

			} else {
				
				cachedTime += cached();
				cachedCount++;

				straightTime += straight();
				straightCount++;
			}
		}

		double straightTotal = (double)straightTime / (double)straightCount;
		double cachedTotal = (double)cachedTime / (double)cachedCount;
		double s2c = straightTotal / cachedTotal;
		logger.info("Total straight/cached "
				+ straightTotal + "/"
				+ cachedTotal + " (" + s2c + ")");
	}
	
	public long straight() {

		long startTime = System.currentTimeMillis();
		
		List<float[]> dump = new LinkedList<float[]>();
		
		for (int count = 0; count < COUNT; count++) {
			
			dump.add(Color.RGBtoHSB(seed.getRed(), seed.getGreen(), seed.getBlue(), null));
		}
		
		long time = System.currentTimeMillis() - startTime;
		
		logger.info("Straight: " + (time) + "ms for " + dump.size() + " items");
		
		return time;
	}

	public long cached() {

		long startTime = System.currentTimeMillis();
		
		List<float[]> dump = new LinkedList<float[]>();
		
		for (int count = 0; count < COUNT; count++) {
			
			dump.add(resolve(seed));
		}
		
		long time = System.currentTimeMillis() - startTime;
		
		logger.info("Cached: " + (time) + "ms for " + dump.size() + " items");
		
		return time;
	}

	private static class RGB2HSB {
		
		public final int rgb;
		public final float hsb[];
		
		public RGB2HSB(int rgb, float[] hsb) {

			this.rgb = rgb;
			this.hsb = hsb;
		}
	}

	private static RGB2HSB[] rgb2hsb = new RGB2HSB[16];

	private float[] resolve(Color color) {
		
		int rgb = color.getRGB();
		int offset = 0;
				
		for (; offset < rgb2hsb.length && rgb2hsb[offset] != null; offset++) {
			
			if (rgb == rgb2hsb[offset].rgb) {

				return rgb2hsb[offset].hsb;
			}
		}
		
		rgb2hsb[offset] = new RGB2HSB(rgb, Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null));
		
		return rgb2hsb[offset].hsb;
	}
}
