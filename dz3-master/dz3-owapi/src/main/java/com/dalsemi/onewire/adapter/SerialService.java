package com.dalsemi.onewire.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.utils.Convert;

/**
 * @author Original implementation &copy; Dallas Semiconductor
 * @author Stability enhancements &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2018
 */
public class SerialService implements SerialPortEventListener {

    protected final static Logger logger = LogManager.getLogger(OneWireAccessProvider.class);

    /**
     * The serial port name of this object (e.g. COM1, /dev/ttyS0).
     */
    private final String portName;

    /**
     * The serial port object for setting serial port parameters.
     */
    private SerialPort serialPort = null;

    /**
     * The input stream, for reading data from the serial port.
     */
    private InputStream serialInputStream = null;

    /**
     * The output stream, for writing data to the serial port.
     */
    private OutputStream serialOutputStream = null;

    /**
     * The lock.
     */
    private final ReentrantLock theLock = new ReentrantLock();

    /**
     * Temporary array, used for converting characters to bytes.
     */
    private byte[] tempArray = new byte[128];

    /**
     * Vector of thread hash codes that have done an open but no close.
     */
    private final Set<Thread> users = new HashSet<Thread>();

    /**
     * Flag to indicate byte banging on read.
     * */
    private final boolean byteBang;

    /**
     * Vector of serial port ID strings (i.e. "COM1", "COM2", etc).
     */
    private static final Vector<String> vPortIDs = new Vector<String>();

    /**
     * Static list of threadIDs to the services they are using.
     */
    private static final Map<Thread, SerialService> knownServices = new HashMap<Thread, SerialService>();

    /**
     * Static list of all unique SerialService classes.
     */
    private static final Map<String, SerialService> uniqueServices = new HashMap<String, SerialService>();


    /**
     * Cleans up the resources used by the thread argument.  If another
     * thread starts communicating with this port, and then goes away,
     * there is no way to relinquish the port without stopping the
     * process. This method allows other threads to clean up.
     *
     * @param thread thread that may have used a {@link USerialAdapter}
     *
     * @deprecated Apparently not used anywhere.
     */
    @Deprecated
    public static void cleanUpByThread(Thread thread) {

        logger.debug("SerialService.CleanUpByThread(Thread)");

        try {

            SerialService serialService = knownServices.get(thread);

            if (serialService == null) {
                return;
            }

            // VT: FIXME: Stuff below doesn't make sense together with changed lock semantics - if you started it, you gotta clean it up...

            /*
      synchronized(serialService) {

        if (thread.hashCode() == serialService.currentThreadHash) {

          //then we need to release the lock...
          serialService.currentThreadHash = 0;
        }
      }
             */

            serialService.closePortByThreadID(thread);

        } catch(Exception ex) {

            logger.error("cleanUpByThread(" + thread + ") failed", ex);
        }
    }

    /**
     * do not use default constructor
     * use getSerialService(String) instead.
     */
    @SuppressWarnings("unused")
    private SerialService() {

        throw new IllegalStateException("Use getSerialService(String) instead");
    }

    /**
     * This constructor is intended to be used by {@link #getSerialService(java.lang.String)}.
     */
    protected SerialService(String portName) {

        this.portName = portName;

        // check to see if need to byte-bang the reads
        String prop = OneWireAccessProvider.getProperty("onewire.serial.bytebangread");

        byteBang = prop != null && prop.contains("true");
    }

    public static SerialService getSerialService(String portName) {
        
        ThreadContext.push("getSerialService");
        
        try {

            synchronized(uniqueServices) {

                logger.debug("requested: " + portName);

                String portId = portName.toLowerCase();
                SerialService existingService = uniqueServices.get(portId);

                if(existingService != null) {
                    return existingService;
                }

                SerialService sps = new SerialService(portName);
                uniqueServices.put(portId, sps);

                return sps;
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * SerialPortEventListener method.  This just calls the notify
     * method on this object, so that all blocking methods are kicked
     * awake whenever a serialEvent occurs.
     */
    public void serialEvent(SerialPortEvent spe) {

        switch(spe.getEventType())
        {
        case SerialPortEvent.BI:
            logger.debug("SerialPortEvent: Break interrupt.");
            break;
        case SerialPortEvent.CD:
            logger.debug("SerialPortEvent: Carrier detect.");
            break;
        case SerialPortEvent.CTS:
            logger.debug("SerialPortEvent: Clear to send.");
            break;
        case SerialPortEvent.DATA_AVAILABLE:
            logger.debug("SerialPortEvent: Data available at the serial port.");
            break;
        case SerialPortEvent.DSR:
            logger.debug("SerialPortEvent: Data set ready.");
            break;
        case SerialPortEvent.FE:
            logger.debug("SerialPortEvent: Framing error.");
            break;
        case SerialPortEvent.OE:
            logger.debug("SerialPortEvent: Overrun error.");
            break;
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            logger.debug("SerialPortEvent: Output buffer is empty.");
            break;
        case SerialPortEvent.PE:
            logger.debug("SerialPortEvent: Parity error.");
            break;
        case SerialPortEvent.RI:
            logger.debug("SerialPortEvent: Ring indicator.");
            break;
        }
        
        logger.debug("SerialService.SerialEvent: oldValue=" + spe.getOldValue());
        logger.debug("SerialService.SerialEvent: newValue=" + spe.getNewValue());
    }


    public synchronized void openPort() throws IOException {

        logger.debug("SerialService.openPort() called");

        openPort(null);
    }

    public synchronized void openPort(SerialPortEventListener spel) throws IOException {
        
        ThreadContext.push("openPort");

        try {

            // record this thread as an owner. It's a Set, extra add() won't hurt
            users.add(Thread.currentThread());

            if(isPortOpen()) {
                return;
            }

            CommPortIdentifier port_id;

            try {
                port_id = CommPortIdentifier.getPortIdentifier(portName);
            } catch(NoSuchPortException ex) {
                throw new IOException(portName + ": no such port", ex);
            }

            // check if the port is currently used
            if (port_id.isCurrentlyOwned()) {
                throw new IOException("Port In Use (" + portName + ")");
            }

            // try to acquire the port
            try {

                // get the port object
                serialPort = (SerialPort) port_id.open("Dallas Semiconductor", 2000);

                //serialPort.setInputBufferSize(4096);
                //serialPort.setOutputBufferSize(4096);

                logger.debug("getInputBufferSize = " + serialPort.getInputBufferSize());
                logger.debug("getOutputBufferSize = " + serialPort.getOutputBufferSize());

                serialPort.addEventListener(spel != null ? spel : this);
                
                serialPort.notifyOnOutputEmpty(true);
                serialPort.notifyOnDataAvailable(true);

                // flow i/o
                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

                serialInputStream  = serialPort.getInputStream();
                serialOutputStream = serialPort.getOutputStream();

                // bug workaround
                serialOutputStream.write(0);

                // settings
                serialPort.disableReceiveFraming();
                serialPort.disableReceiveThreshold();
                serialPort.enableReceiveTimeout(1);

                // set baud rate
                serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                serialPort.setDTR(true);
                serialPort.setRTS(true);

                logger.debug("Port Opened (" + portName + ")");

            } catch(Exception ex) {

                // close the port if we have an object
                if (serialPort != null) {
                    serialPort.close();
                }

                serialPort = null;

                throw new IOException("Could not open port (" + portName + ")", ex);
            }
        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized void setNotifyOnDataAvailable(boolean notify) {
        serialPort.notifyOnDataAvailable(notify);
    }

    @SuppressWarnings("unchecked")
    public static Enumeration<String> getSerialPortIdentifiers() {

        synchronized(vPortIDs) {

            if(vPortIDs.isEmpty()) {

                for (Enumeration<CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers(); e.hasMoreElements(); ) {

                    CommPortIdentifier portID = e.nextElement();

                    if (portID.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                        vPortIDs.add(portID.getName());
                    }
                }
            }

            return vPortIDs.elements();
        }
    }

    public synchronized String getPortName() {
        return portName;
    }

    public synchronized boolean isPortOpen() {
        return serialPort!=null;
    }

    public synchronized boolean isDTR() {
        return serialPort.isDTR();
    }

    public synchronized void setDTR(boolean newDTR) {
        serialPort.setDTR(newDTR);
    }

    public synchronized boolean isRTS() {
        return serialPort.isRTS();
    }

    public synchronized void setRTS(boolean newRTS) {
        serialPort.setRTS(newRTS);
    }

    /**
     * Send a break on this serial port.
     *
     * @param  duration - break duration in ms.
     */
    public synchronized void sendBreak(int duration){
        serialPort.sendBreak(duration);
    }

    public synchronized int getBaudRate() {
        return serialPort.getBaudRate();
    }

    public synchronized void setBaudRate(int baudRate) throws IOException {

        ThreadContext.push("setBaudRate(" + baudRate + ")");
        
        try {
            if(!isPortOpen())
                throw new IOException(null, new IllegalStateException("Port Not Open"));

            try {
                // set baud rate
                serialPort.setSerialPortParams(baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                logger.debug("Set baudRate=" + baudRate);

            } catch(UnsupportedCommOperationException ex) {
                throw new IOException("Failed to set baud rate: ", ex);
            }
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Close this serial port.
     */
    public synchronized void closePort() {

        logger.debug("SerialService.closePort");

        closePortByThreadID(Thread.currentThread());
    }

    public synchronized void flush() throws IOException {

        logger.debug("SerialService.flush");

        if (!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        serialOutputStream.flush();

        while(serialInputStream.available() > 0) {
            serialInputStream.read();
        }
    }

    /**
     * Gets exclusive use of the 1-Wire to communicate with an iButton or
     * 1-Wire Device.
     *
     * This method should be used for critical sections of code where a
     * sequence of commands must not be interrupted by communication of
     * threads with other iButtons, and it is permissible to sustain
     * a delay in the special case that another thread has already been
     * granted exclusive access and this access has not yet been
     * relinquished.
     */
    public void beginExclusive() {

        logger.debug("SerialService.beginExclusive()");

        theLock.lock();
    }

    /**
     * Relinquishes exclusive control of the 1-Wire Network.
     * This command dynamically marks the end of a critical section and
     * should be used when exclusive control is no longer needed.
     */
    public synchronized void endExclusive () {

        logger.debug("SerialService.endExclusive");

        theLock.unlock();
    }

    /**
     * Allows clean up port by thread.
     */
    private synchronized void closePortByThreadID(Thread t) {
        
        ThreadContext.push("closePortByThreadID");

        try {

            logger.debug("SerialService.closePortByThreadID(Thread), Thread=" + t);

            // remove this thread as an owner
            users.remove(t);

            // if this is the last owner then close the port
            if (users.isEmpty()) {

                // if don't own a port then just return
                if (!isPortOpen()) {
                    return;
                }

                // close the port
                serialPort.close();
                serialPort = null;
                serialInputStream = null;
                serialOutputStream = null;
            }
            
        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized int available() throws IOException {

        if(!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        return serialInputStream.available();
    }

    public synchronized int read() throws IOException {

        if (!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        return serialInputStream.read();
    }

    public synchronized int read(byte[] buffer) throws IOException {

        if(!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        return read(buffer, 0, buffer.length);
    }

    public synchronized int read(byte[] buffer, int offset, int length) throws IOException {

        if(!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        return serialInputStream.read(buffer, offset, length);
    }

    public synchronized int readWithTimeout(byte[] buffer, int offset, int length) throws IOException {
        
        ThreadContext.push("readWithTimeout");
        
        try {

            if (!isPortOpen()) {
                throw new IOException(null, new IllegalStateException("Port Not Open"));
            }

            // set timeout to be very long
            long timeout = System.currentTimeMillis() + length*20 + 800;

            logger.debug("SerialService.readWithTimeout(): length=" + length + ", timeout=" + timeout);


            int count = byteBang
            ? readWithTimeoutByteBang(buffer, offset, length, timeout)
                    : readWithTimeoutNoByteBang(buffer, offset, length, timeout);

            logger.debug("SerialService.readWithTimeout: read " + count + " bytes");
            logger.debug("SerialService.readWithTimeout: " + Convert.toHexString(buffer, offset, count));

            return count;

        } finally {
            ThreadContext.pop();
        }
    }

    private int readWithTimeoutByteBang(byte[] buffer, int offset, int length, long timeout) throws IOException {
        
        ThreadContext.push("readWithTimeoutByteBang");
        
        int count = 0;
        try {


            do {

                int new_byte = serialInputStream.read();

                if (new_byte != -1) {

                    buffer[count+offset] = (byte)new_byte;
                    count++;

                } else {

                    if (System.currentTimeMillis() > timeout) {
                        logger.debug("premature return, timeout (" + timeout + ") exceeded");
                        return count;
                    }

                    // no bytes available yet so yield
                    Thread.yield();

                    logger.debug("yield ended");
                }

            } while (length > count);

            return count;

        } finally {
            logger.debug("returning " + count);
            ThreadContext.pop();
        }
    }

    private int readWithTimeoutNoByteBang(byte[] buffer, int offset, int length, long timeout) throws IOException {

        int count = 0;

        do {

            int available = serialInputStream.available();

            if (available > 0) {

                // check for block bigger then buffer
                if (available + count > length) {
                    available = length - count;
                }

                // read the block
                count += serialInputStream.read(buffer, count + offset, available);

            } else {

                // check for timeout
                if (System.currentTimeMillis() > timeout  ) {
                    length = 0;
                }

                Thread.yield();
            }

        } while (length > count);

        return count;
    }

    public synchronized char[] readWithTimeout(int length) throws IOException {

        byte[] buffer = new byte[length];

        int count = readWithTimeout(buffer, 0, length);

        if (length != count) {
            throw new IOException("readWithTimeout, timeout waiting for return bytes (wanted " + length + ", got " + count + ")");
        }

        char[] returnBuffer = new char[length];

        for(int i = 0; i < length; i++) {
            returnBuffer[i] = (char) (buffer[i] & 0x00FF);
        }

        return returnBuffer;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void write(int data) throws IOException {
        
        ThreadContext.push("write");

        if(!isPortOpen()) {
            throw new IOException(null, new IllegalStateException("Port Not Open"));
        }

        logger.debug("data: " + Convert.toHexString((byte)data));

        try {

            serialOutputStream.write(data);
            serialOutputStream.flush();

        } catch (IOException e) {

            // drain IOExceptions that are 'Interrrupted' on Linux
            // convert the rest to IOExceptions

            if (!(System.getProperty("os.name").contains("Linux")
                    && e.toString().contains("Interrupted"))) {
                throw new IOException("write(char): " + e);
            }
        }
    }

    public synchronized void write(byte[] data, int offset, int length) throws IOException {
        
        ThreadContext.push("write");
        
        try {

            if (!isPortOpen()) {
                throw new IOException("Port Not Open");
            }

            logger.debug("length: " + length + " bytes");
            logger.debug("data: " + Convert.toHexString(data, offset, length));

            try {

                serialOutputStream.write(data, offset, length);
                serialOutputStream.flush();

            } catch (IOException e) {

                // drain IOExceptions that are 'Interrrupted' on Linux
                // convert the rest to IOExceptions

                if (!((System.getProperty("os.name").indexOf("Linux") != -1)
                        && (e.toString().indexOf("Interrupted") != -1))) {
                    throw new IOException("write(char): " + e);
                }
            }

        } finally {
            ThreadContext.pop();
        }
    }

    public synchronized void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public synchronized void write(String data) throws IOException {
        byte[] dataBytes = data.getBytes();
        write(dataBytes, 0, dataBytes.length);
    }

    public synchronized void write(char data) throws IOException {
        write((int)data);
    }

    public synchronized void write(char[] data) throws IOException {
        write(data, 0, data.length);
    }

    public synchronized void write(char[] data, int offset, int length) throws IOException {
        
        ThreadContext.push("write");
        
        try {

            if (length > tempArray.length) {
                
                logger.warn("Extending temp buffer to " + length + " bytes");
                
                tempArray = new byte[length];
            }

            for (int i=0; i<length; i++) {
                tempArray[i] = (byte) data[i];
            }

            write(tempArray, 0, length);
        
        } finally {
            ThreadContext.pop();
        }
    }
}
