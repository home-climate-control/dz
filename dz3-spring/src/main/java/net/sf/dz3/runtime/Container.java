package net.sf.dz3.runtime;

import net.sf.dz3.instrumentation.Marker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Entry point into DZ Core.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class Container {

    /**
     * Logger to use.
     */
    private final static Logger logger = LogManager.getLogger(Container.class);

    /**
     * Name of the configuration file embedded into the jar file.
     */
    public static final String CF_EMBEDDED = "spring-config.xml";
    
    /**
     * Name of the file this class expects to find the configuration in.
     * 
     * Must be on the root of the classpath.
     */
    public static final String CF_PI = "raspberry-pi.xml";
    
    /**
     * @param args None expected.
     */
    public static void main(String[] args) {
        
        new Container().run(args);
    }

    /**
     * Run the system.
     */
    public void run(String[] args) {

        ThreadContext.push("run");

        try {

            boolean configFound = false;

            if (args.length == 0) {

                logger.warn("Usage: dz <configuration file>");
                logger.warn("Trying to load a sample configuration from '{}'", CF_PI);

                configFound = loadConfiguration(CF_PI);

            } else {
                for (String arg : args) {
                    configFound = loadConfiguration(arg) || configFound;
                }
            }

            if (!configFound) {
                logger.error("No configuration was found, terminating");
                return;
            }

            logger.info("Sleeping until killed");
            
            while (true) {
                Thread.sleep(10000);
            }

        } catch (Throwable t) {
            logger.fatal("Unexpected exception: ", t);
        } finally {
            
            logger.fatal("Shutting down");
            ThreadContext.pop();
        }
    }

    /**
     * Load the configuration.
     * 
     * @param source Configuration source.
     * 
     * @see #CF_PI
     */
    private boolean loadConfiguration(String source) {

        ThreadContext.push("loadConfiguration(" + source + ")");
        Marker m = new Marker("loadConfiguration(" + source + ")");

        try {

            // Classpath loading is much less likely, let's try this first
            if (loadFromPath(source)) {
                return true;
            };

            return loadFromClasspath(source);

        } finally {

            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Load the configuration from a file.
     *
     * @param source File to load the configuration from.
     * @return {@code true} if an attempt succeeded.
     */
    private boolean loadFromPath(String source) {
        try {

            if (source.startsWith("/")) {
                logger.warn("Absolute location, using Spring file: quirk");
                source = "file:" + source;
            }

            AbstractApplicationContext applicationContext = new FileSystemXmlApplicationContext(new String[]{source});

            applicationContext.registerShutdownHook();
            return true;

        } catch (BeanDefinitionStoreException ex) {

            logger.warn(String.format("Failed to load %s, reason: %s", source, ex.getMessage()));
            return false;
        }
    }

    private boolean loadFromClasspath(String source) {
        try {

            AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[]{source});

            applicationContext.registerShutdownHook();
            return true;

        } catch (BeanDefinitionStoreException ex) {

            logger.warn(String.format("Failed to load %s, reason: %s", source, ex.getMessage()));
            return false;
        }
    }
}
