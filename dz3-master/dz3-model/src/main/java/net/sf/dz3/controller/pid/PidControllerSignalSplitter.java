package net.sf.dz3.controller.pid;

import org.apache.log4j.NDC;

import net.sf.dz3.controller.ProcessControllerSignalSplitter;
import net.sf.dz3.controller.ProcessControllerStatus;
import net.sf.jukebox.datastream.signal.model.DataSample;

/**
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class PidControllerSignalSplitter extends ProcessControllerSignalSplitter {
    
    /**
     * Create an instance not attached to anything.
     */
    public PidControllerSignalSplitter() {

    }

    /**
     * Create an instance and add it as a listener to the given source.
     * 
     * @param controller Controller to listen to.
     */
    public PidControllerSignalSplitter(AbstractPidController controller) {
        super(controller);
    }

    /**
     * {@inheritDoc}
     */
    public void consume(DataSample<ProcessControllerStatus> signal) {
        
        // Let them consume the common process controller signal components
        super.consume(signal);
        
        // And now let's take care of PID specific components
        
        NDC.push("consume.pid");
        
        try {
            
            // This is a bit dangerous, but let's see how exactly dangerous 
            PidControllerStatus pidStatus = (PidControllerStatus) signal.sample; 
            
            long timestamp = signal.timestamp;
            String sourceName = signal.sourceName;
            
            consume(timestamp, sourceName + ".p", pidStatus.p);
            consume(timestamp, sourceName + ".i", pidStatus.i);
            consume(timestamp, sourceName + ".d", pidStatus.d);
            
        } finally {
            NDC.pop();
        }
    }
}
