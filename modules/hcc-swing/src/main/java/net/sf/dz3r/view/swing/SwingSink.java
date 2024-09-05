package net.sf.dz3r.view.swing;

import com.homeclimatecontrol.hcc.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import javax.swing.JPanel;

/**
 * Generic component capable of accepting a {@link Signal} {@link Flux}.
 *
 * @param <T> Signal value type.
 * @param <P> Extra payload type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2022
 */
public abstract class SwingSink<T, P> extends JPanel {

    protected final transient Logger logger = LogManager.getLogger();

    private transient Signal<T, P> signal;

    public final void consumeSignal(Signal<T,P> signal) {

        this.signal = signal;
        logger.debug("signal: {}", signal);

        if (update(signal)) {
            repaint();
        }
    }

    /**
     * Update the visualization.
     *
     * @param signal Signal to consume.
     * @return {@code true} if a repaint is necessary.
     */
    protected abstract boolean update(Signal<T,P> signal);

    protected final Signal<T, P> getSignal() {
        return signal;
    }

    protected boolean isOK() {
        return getSignal() != null && getSignal().isOK();
    }

    protected boolean isError() {
        return getSignal() == null || getSignal().isError();
    }
}
