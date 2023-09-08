package net.sf.dz3.runtime.config.hardware;

import java.time.Duration;

/**
 * Configuration entry for switches.
 *
 * @param id Identifier, optional (defaults to {@link #address} if absent).
 * @param address Device address. Mandatory.
 * @param reversed {@code true} if the switch must be reversed.
 * @param heartbeat Issue control commands to this switch at least this often, repeat if necessary.
 * @param pace Issue identical control commands to this switch at most this often.
 * @param optimistic See <a href="https://github.com/home-climate-control/dz/issues/280">issue 280</a>.
 */
public record SwitchConfig(
        String id,
        String address,
        boolean reversed,
        Duration heartbeat,
        Duration pace,
        Boolean optimistic
) {
}
