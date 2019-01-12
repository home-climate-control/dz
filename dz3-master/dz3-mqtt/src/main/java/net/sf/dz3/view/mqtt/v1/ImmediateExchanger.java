package net.sf.dz3.view.mqtt.v1;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.ThreadContext;

public abstract class ImmediateExchanger<DataBlock> extends AbstractExchanger<DataBlock> {

    public ImmediateExchanger(BlockingQueue<DataBlock> upstreamQueue) {
        super(upstreamQueue);
    }
    

    @Override
    public void run() {

        ThreadContext.push("run");
        
        try {

            while (true) {
                
                try {
                    
                    exchange(upstreamQueue.take());
                    
                } catch (InterruptedException ex) {
                    
                    logger.info("interrupted, terminating");
                    return;

                } catch (Throwable t) {
                    
                    // Can't afford to bail out, this may be a transient condition
                    logger.error("Unexpected exception", t);
                }    
            }
            
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Exchange information with the server.
     * 
     * Information received from the server will be processed asynchronously.
     * 
     * @param dataBlock Block to send.
     * 
     * @throws IOException if things go sour.
     */
    protected final void exchange(DataBlock dataBlock) throws IOException {
        
        ThreadContext.push("exchange");
        
        try {
            
            send(dataBlock);
            
            // VT: FIXME: Process the response
            
        } finally {
            ThreadContext.pop();
        }
    }

    protected abstract void send(DataBlock dataBlock) throws IOException;
}
