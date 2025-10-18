package net.sf.dz3r.runtime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Provides the application private storage location.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2025
 */
public class AppHome {

    private static final Logger logger = LogManager.getLogger(AppHome.class);
    private static final String HOME = ".hcc";
    private static File home;

    private AppHome() {

    }

    public synchronized static File getHome() {

        if (home == null) {
            home = new File(new File(System.getProperty("user.home")), HOME);
            logger.debug("home={}", home);
        }

        return home;
    }
}
