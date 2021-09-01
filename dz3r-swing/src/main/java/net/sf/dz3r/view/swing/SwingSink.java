package net.sf.dz3r.view.swing;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import javax.swing.JPanel;

public class SwingSink<T, P> extends JPanel {

    protected final transient Logger logger = LogManager.getLogger();

    private transient Signal<T, P> signal;

    public final void subscribe(Flux<Signal<T, P>> source) {
        source.subscribe(this::consume);
    }

    private void consume(Signal<T,P> signal) {
        this.signal = signal;
        logger.info("signal: {}", signal);
        update();
    }

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
