package net.sf.dz3r.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Provides a persistent unique instance identifier.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class InstanceIdProvider {

    private static final Logger logger = LogManager.getLogger(InstanceIdProvider.class);
    private static final File source = new File(System.getProperty("user.home"), ".dz/system.id");
    private static UUID id;

    private InstanceIdProvider() {

    }

    /**
     * Return a unique ID; generate if necessary.
     *
     * @return Unique instance ID to be used as this system's identifier across its integrations.
     */
    public static synchronized UUID getId() throws IOException {

        if (id == null) {

            id = readId();

            if (id == null) {
                id = generateId();
            }
        }

        return id;
    }

    /**
     * Read the persistent ID from a known location.
     *
     * @return The UUID stored, or {@code null} if there was none.
     * @throws IOException if things went sour; let's fast fail here.
     */
    private static UUID readId() throws IOException {
        try (var br = new BufferedReader(new FileReader(source, StandardCharsets.UTF_8))) {
            return UUID.fromString(br.readLine());
        } catch (FileNotFoundException ex) {

            // In case there was something insidious
            logger.debug("readId failed", ex);

            // For public consumption
            logger.info("no persistent ID found at {}, generating a new one", source);

            return generateId();
        }
    }

    /**
     * Generate, store, and return a new persistent ID.
     *
     * @return An ID that is guaranteed to have been persisted.
     */
    private static UUID generateId() throws IOException {

        var newId = UUID.randomUUID();

        try (PrintWriter pw = new PrintWriter(new FileWriter(source))) {
            pw.println(newId);
        }

        logger.info("new unique system ID generated: {}", newId);

        return newId;
    }
}
