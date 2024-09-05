package net.sf.dz3r.signal.health;

import com.homeclimatecontrol.hcc.signal.Signal;

import java.time.Duration;

/**
 * Status of any polling connector.
 *
 * Note that this object bears no failure indication, this is what {@link Signal#status} is for.
 *
 * @param period Detected average period.
 * @param periodStDev Detected period standard deviation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record PollingConnectorStatus(
        Duration period,
        Duration periodStDev
) implements ConnectorStatus {
}
