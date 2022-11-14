package net.sf.dz3r.view.swing;

import net.sf.dz3r.signal.Signal;
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

    public final void subscribe(Flux<Signal<T, P>> source) {
        source.subscribe(this::consumeSignal);
    }

    // VT: FIXME: This must be private and driven via Flux only
    public void consumeSignal(Signal<T,P> signal) {

        this.signal = signal;
        logger.debug("signal: {}", signal);

        consumeSignalValue(signal.getValue());
        update();
    }

    protected abstract void consumeSignalValue(T value);

    /**
     * Update the display and repaint.
     */
    protected void update() {

        // Default behavior. May or may not be suitable for subclasses.
        repaint();
    }

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
