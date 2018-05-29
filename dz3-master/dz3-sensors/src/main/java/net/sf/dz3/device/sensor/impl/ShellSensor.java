package net.sf.dz3.device.sensor.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.ThreadContext;

import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.jmx.JmxAttribute;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Get a reading returned by a shell command.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2018
 */
public class ShellSensor extends AbstractAnalogSensor {

    /**
     * Shell command to execute.
     */
    private final String command;
    
    /**
     * Create an instance.
     *  
     * @param address Sensor hardware address. Recommended value is the host name,
     * followed by a colon, followed by the postfix. Rationale for this is that the sensr
     * reading may be propagated beyond the host, and it is quite possible that the same
     * measured entity will be present on different hosts as well.
     * 
     * @param pollInterval How often the value needs to be delivered. Recommended value for this class is
     * no less than 1000ms.
     *  
     * @param command Shell command to execute. May be arbitrarily complex, must return a value that is
     * parseable into {@code double}.
     */
    public ShellSensor(String address, int pollInterval, String command) {

        super(address, pollInterval);
        
        if (command == null || "".equals(command)) {
            
            // Unfortunately, this is as good as it gets. Or is it?
            throw new IllegalArgumentException("command can't be null or empty");
        }
        
        this.command = command;
    }

    @Override
    public DataSample<Double> getSensorSignal() throws IOException {
        
        ThreadContext.push("getSensorTemperature#" + Integer.toHexString(hashCode()));

        BufferedReader br = null;
        
        long timestamp = System.currentTimeMillis();

        try {
            
            Process p = null;
            
            try {

                logger.debug("Executing: '/bin/sh -c " + command + "'");

                p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});

                int rc = p.waitFor();

                if (rc != 0) {
                    // We're screwed
                    logger.error("Command returned error code " + rc + ": "
                            + command);
                }

                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String output = read(br);
                
                logger.debug("Output: " + output);
                
                if (rc == 0) {
                    
                    double sample = Double.parseDouble(output);
                    return new DataSample<Double>(timestamp, getAddress(), getAddress(), sample, null);
                    
                } else {
                    
                    Throwable t = new IOException("rc=" + rc + ", output: " + output);
                    return new DataSample<Double>(timestamp, getAddress(), getAddress(), null, t);
                }
                
            } finally {

                // Unless this is executed, repeated invocations of exec() will
                // eventually cause the system to run out of file handles
                
                if (p != null) {

                    p.destroy();
                }
            }

        } catch (Throwable t) {
            
            return new DataSample<Double>(timestamp, getAddress(), getAddress(), null, t);
            
        } finally {
            
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.info("Can't close() the process stream, ignored:", e);
                }
            }
            
            ThreadContext.pop();
        }
    }

    /**
     * Parse the content of the reader into a string.
     * 
     * @param br Reader to read from.
     * 
     * @return Parsed string.
     * 
     * @exception IOException if things go sour.
     */
    private String read(BufferedReader br) throws IOException {
        
        ThreadContext.push("read");
        
        try {

            long size = 0;
            StringBuilder sb = new StringBuilder();

            while (true) {

                String line = br.readLine();

                if (line == null) {
                    break;
                }
                
                if (size > 0) {
                    sb.append(System.getProperty("line.separator"));
                }

                size += line.length();
                sb.append(line);
            }

            return sb.toString();

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void startup() throws Throwable {

        ThreadContext.push("startup");
        
        try {
        
            logger.info("starting '" + command + "'");
            
        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    protected void shutdown() throws Throwable {
        
        ThreadContext.push("shutdown");
        
        try {
        
            logger.info("stopping '" + command + "'");
            
        } finally {
            ThreadContext.pop();
        }
    }
    
    @JmxAttribute(description="Shell command being executed")
    public String getCommand() {
        
        return command;
    }

    /**
     * {@inheritDoc}
     */
    public JmxDescriptor getJmxDescriptor() {
        
      return new JmxDescriptor(
              "dz",
              getClass().getSimpleName(),
              Integer.toHexString(hashCode()),
              "Execute a shell command, treat stdout as the reading");
    }
}
