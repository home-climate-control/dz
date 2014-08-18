package net.sf.dz3.view.http.common;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.sf.dz3.view.http.v1.UpstreamBlock;

/**
 * Basic contraption to feed the {@link #upstreamQueue}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public abstract class QueueFeeder<DataBlock> {
    
    /**
     * Key to extract the {@link #upstreamQueue queue} from the map given to the constructor.
     */
    public final static String QUEUE_KEY = "upstream queue";

    /**
     * Queue to put notifications into.
     * 
     *  {@link #emit(UpstreamBlock)} will take care of that.
     */
    private final BlockingQueue<DataBlock> upstreamQueue;
    
    /**
     * Create an instance.
     * 
     * @param context A map that contains the {@link #upstreamQueue} object under
     * {@link #QUEUE_KEY} key. 
     */
    @SuppressWarnings("unchecked")
    public QueueFeeder(Map<String, Object> context) {

        upstreamQueue = (BlockingQueue<DataBlock>) context.get(QUEUE_KEY);
    }

    /**
     * Queue the notification.
     * 
     * @param b Data block to queue.
     */
    protected final void emit(DataBlock b) {
        
        upstreamQueue.add(b);
    }
}
