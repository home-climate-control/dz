package net.sf.dz3.view.mqtt.v1;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import net.sf.jukebox.util.MessageDigestFactory;

/**
 * Basic contraption to feed the {@link #upstreamQueue}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public abstract class QueueFeeder<DataBlock> {
    
    /**
     * Key to extract the {@link #upstreamQueue queue} from the map given to the constructor.
     */
    public final static String QUEUE_KEY = "upstream queue";

    private final MessageDigestFactory digestFactory = new MessageDigestFactory();

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

        if (context == null) {
            throw new IllegalArgumentException("null context, doesn't make sense");
        }

        upstreamQueue = (BlockingQueue<DataBlock>) context.get(QUEUE_KEY);

        if (upstreamQueue == null) {
            throw new IllegalArgumentException("null queue, doesn't make sense");
        }
    }

    /**
     * Queue the notification.
     * 
     * @param b Data block to queue.
     */
    protected final void emit(DataBlock b) {
        
        upstreamQueue.add(b);
    }

    protected String getMessageDigest(String source) {

        // VT: NOTE: MD5 is cryptographically weak, but probably sufficient for this purpose

        return digestFactory.getMD5(source);
    }
}
