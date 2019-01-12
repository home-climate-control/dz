package net.sf.dz3.view.mqtt.v1;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractExchanger<DataBlock> implements Runnable {

    protected final Logger logger = LogManager.getLogger(getClass());
    protected final BlockingQueue<DataBlock> upstreamQueue;

    public AbstractExchanger(BlockingQueue<DataBlock> upstreamQueue) {

        this.upstreamQueue = upstreamQueue;
    }
}
