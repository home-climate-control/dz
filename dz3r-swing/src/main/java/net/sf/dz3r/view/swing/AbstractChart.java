package net.sf.dz3r.view.swing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.time.Clock;

public class AbstractChart<T, P> extends SwingSink<T, P> {

    protected static final Stroke strokeSingle = new BasicStroke();
    protected static final Stroke strokeDouble = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);

    protected final transient Logger logger = LogManager.getLogger();

    /**
     * Grid color.
     *
     * Default is dark gray.
     */
    protected static final Color gridColor = Color.darkGray;

    /**
     * Clock used.
     */
    protected final transient Clock clock;

    /**
     * Chart length, in milliseconds.
     */
    protected long chartLengthMillis;

    protected AbstractChart(Clock clock, long chartLengthMillis) {

        if (chartLengthMillis < 1000 * 10) {
            throw new IllegalArgumentException("Unreasonably short chart length " + chartLengthMillis + "ms");
        }

        this.clock = clock;
        this.chartLengthMillis = chartLengthMillis;
    }
}
