package net.sf.dz3.device.sensor.impl.onewire;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.DzSwitchContainer;
import net.sf.dz3.device.sensor.SensorType;
import net.sf.jukebox.instrumentation.Marker;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.SwitchContainer;

/**
 * A platform independent switch container.
 */
public class OneWireSwitchContainer extends OneWireDeviceContainer implements DzSwitchContainer {
    
    private final Logger logger = LogManager.getLogger(getClass());
    private final OwapiDeviceFactory factory;

    /**
     * Number of channels the device has.
     */
    private int channelCount = 0;

    /**
     * Create an instance.
     * 
     * @param container
     *            1-Wire API container to base this container on.
     */
    public OneWireSwitchContainer(final OwapiDeviceFactory factory, final OneWireContainer container) {

        super(container);
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final SensorType getType() {

        return SensorType.SWITCH;
    }

    /**
     * @return Number of channels the device has.
     */
    public final synchronized int getChannelCount() {

        if (channelCount == 0) {

            // This means that we haven't read the device yet

            ReentrantReadWriteLock lock = null;

            String address = container.getAddressAsString();
            SwitchContainer sc = (SwitchContainer) container;

            try {

                // VT: FIXME: Consider replacing this with a Marker
                long start = System.currentTimeMillis();

                lock = factory.getLock();
                lock.writeLock().lock();

                long gotLock = System.currentTimeMillis();

                factory.getDevicePath(address).open();

                byte[] state = sc.readDevice();

                channelCount = sc.getNumberChannels(state);

                long now = System.currentTimeMillis();
                logger.info(address + " has " + channelCount
                        + " channel[s], took us " + (now - start)
                        + "ms to figure out (" + (gotLock - start)
                        + " to get the lock, " + (now - gotLock)
                        + " to retrieve)");

            } catch (Throwable t) {

                logger.warn(address + ": can't retrieve channel count (assuming 2):", t);

                channelCount = 2;

            } finally {

                if (lock != null) {

                    lock.writeLock().unlock();
                }
            }
        }

        return channelCount;
    }

    /**
     * Read channel.
     * 
     * @param channel Channel to read.
     * @exception IOException if there was a problem reading the device.
     * @return Channel value.
     */
    public final boolean read(final int channel) throws IOException {

        ReentrantReadWriteLock lock = null;
        SwitchContainer sc = (SwitchContainer) container;
        String address = container.getAddressAsString();
        
        ThreadContext.push("read(" + address + ":" + channel + ")");
        Marker m = new Marker("read(" + address + ":" + channel + ")");

        try {

            lock = factory.getLock();

            lock.writeLock().lock();
            m.checkpoint("got lock");

            factory.getDevicePath(address).open();

            byte[] state = sc.readDevice();

            m.checkpoint("readDevice");

            // boolean smart = sc.hasSmartOn();
            
            boolean result = sc.getLatchState(channel, state);
            
            logger.debug("state=" + result);

            return result;

        } catch (Throwable t) {

            IOException secondary = new IOException("Unable to read " + container);

            secondary.initCause(t);

            throw secondary;

        } finally {

            if (lock != null) {

                lock.writeLock().unlock();
            }

            m.close();
            ThreadContext.pop();
        }
    }

    /**
     * Write channel.
     * 
     * @param channel
     *            Channel to write.
     * @param value
     *            Value to write.
     * @exception IOException
     *                if there was a problem writing to the device.
     */
    public final void write(final int channel, final boolean value)
    throws IOException {

        ReentrantReadWriteLock lock = null;

        SwitchContainer sc = (SwitchContainer) container;
        String address = container.getAddressAsString();

        ThreadContext.push("write(" + address + ":" + channel + ", " + value + ")");
        Marker m = new Marker("write(" + address + ":" + channel + ", " + value + ")");

        try {

            lock = factory.getLock();

            lock.writeLock().lock();
            m.checkpoint("got lock");

            factory.getDevicePath(address).open();

            byte[] state = sc.readDevice();

            m.checkpoint("readDevice/1");
            
            if (logger.isDebugEnabled()) {
                logger.debug("state: " + dumpState(state));
            }

            boolean smart = sc.hasSmartOn();

            sc.setLatchState(channel, value, smart, state);

            m.checkpoint("writeDevice");
            
            if (logger.isDebugEnabled()) {
                logger.debug("state: " + dumpState(state));
            }
            
            sc.writeDevice(state);
            state = sc.readDevice();

            m.checkpoint("readDevice/2");

            if (logger.isDebugEnabled()) {
                logger.debug("state: " + dumpState(state));
            }

            if (value == sc.getLatchState(channel, state)) {

                return;
            }

            logger.error("Failed to write " + container);

        } catch (Throwable t) {

            IOException secondary = new IOException("Unable to write " + container);

            secondary.initCause(t);

            throw secondary;

        } finally {

            if (lock != null) {

                lock.writeLock().unlock();
            }

            m.close();
            ThreadContext.pop();
        }
    }

    private String dumpState(byte[] state) {
        
        StringBuilder sb = new StringBuilder();
        
        if (state == null) {
            
            sb.append("<null>");
            
        } else {
            
            sb.append("(");
            
            for (int offset = 0; offset < state.length; offset++) {
                
                if (offset > 0) {
                    sb.append(" ");
                }

                sb.append("0x").append(Integer.toHexString(state[offset] & 0xFF));
            }
            
            sb.append(")");
        }
        
        return sb.toString();
    }

    /**
     * Reset the device. In other words, set all channels to 0.
     * 
     * @exception IOException
     *                if there was an exception writing the device.
     */
    public final void reset() throws IOException {

        // VT: FIXME: Can't afford to get the lock to improve latency -
        // it will deadlock. In order to fix that, RWLock has to be
        // modified to take multiple calls from the same thread into
        // account.

        for (int channel = 0; channel < getChannelCount(); channel++) {

            write(channel, false);
        }
    }
}
