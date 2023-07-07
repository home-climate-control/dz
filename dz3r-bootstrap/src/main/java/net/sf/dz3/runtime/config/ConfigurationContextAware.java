package net.sf.dz3.runtime.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ConfigurationContextAware {

    protected final Logger logger = LogManager.getLogger();
    protected final ConfigurationContext context;

    protected ConfigurationContextAware(ConfigurationContext context) {
        this.context = context;
    }
}
