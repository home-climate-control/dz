package net.sf.dz3r.view.swing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.Duration;

/**
 * Reactive adapter for the {@link KeyListener} interface.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class KeyFlux implements KeyListener {

    private final Sinks.Many<KeyEvent> sink;
    public final Flux<KeyEvent> flux;

    public KeyFlux() {
        sink = Sinks.many().replay().limit(Duration.ofMinutes(1));
        flux = sink.asFlux();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        sink.tryEmitNext(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        sink.tryEmitNext(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        sink.tryEmitNext(e);
    }
}
