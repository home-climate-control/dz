package net.sf.dz3.device.sensor.impl.tcp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.net.ssl.SSLException;

import net.sf.dz3.device.sensor.TemperatureSensor;
import net.sf.dz3.device.sensor.impl.AbstractAnalogSensor;
import net.sf.dz3.device.sensor.impl.tcp.TcpConnectionSignature;
import net.sf.dz3.util.SSLContextFactory;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.sem.SemaphoreGroup;
import net.sf.jukebox.service.ActiveService;
import net.sf.jukebox.service.PassiveService;

import org.apache.log4j.NDC;

/**
 * TCP temperature sensor.
 *
 * <p>
 *
 * Connects to DAC over TCP and reads temperature values, then distributes
 * them to listeners.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2004
 * @version $Id: TcpTemperatureSensor.java,v 1.10 2007-03-01 21:34:26 vtt Exp $
 */
public class TcpSensorFactory extends PassiveService {

    /**
     * TCP reader map.
     */
    private final Map<ReaderSignature, TcpReader> sig2reader = new TreeMap<ReaderSignature, TcpReader>();
    
    /**
     * Consumer map.
     * 
     * We cheat by not associating the hardware sensor address with the host name and port, and thus enable individual
     * devices to be moved from one host to another.
     */
    private final Map<String, TcpTemperatureSensor> address2sensor = new TreeMap<String, TcpTemperatureSensor>(); 
    
    /**
     * Get a sensor instance using insecure connection to remote port 5000.
     * 
     * @param address Sensor hardware address.
     * @param remoteHost Host to connect to.
     * @param port Port on the remote host to connect to.
     * @param pollInterval How often to return results.
     * 
     * @return A sensor instance.
     */
    public TemperatureSensor getInstance(String address, String remoteHost, int pollInterval) {
    
        return getInstance(address, remoteHost, 5000, pollInterval, false, null);
    }

    /**
     * Get a sensor instance.
     * 
     * @param address Sensor hardware address.
     * @param remoteHost Host to connect to.
     * @param remotePort Port on the remote host to connect to.
     * @param pollInterval How often to return results.
     * @param secure Whether to attempt a secure connection.
     * @param password Password for a secure connection, {@code null} if not secure.
     * 
     * @return A sensor instance.
     */
    public synchronized TemperatureSensor getInstance(String address, String remoteHost, int remotePort, int pollInterval, boolean secure, String password) {
        
        ReaderSignature signature = new ReaderSignature(remoteHost, remotePort, secure, password);
        TcpReader listener = sig2reader.get(signature);
        
        if (listener == null) {
            
            listener = new TcpReader(signature);
            
            // We don't give a damn if it actually starts successfully, it will be reflected
            // in the data sample as an error if it doesn't
            listener.start();
            
            sig2reader.put(signature, listener);
        }
        
        return listener.getInstance(address);
    }

    @Override
    protected void startup() throws Throwable {

    }

    @Override
    protected void shutdown() throws Throwable {
        
        NDC.push("shutdown");
        
        try {
            
            SemaphoreGroup stopped = new SemaphoreGroup();

            for (Iterator<TcpReader> i = sig2reader.values().iterator(); i.hasNext(); ) {

                TcpReader reader = i.next();

                logger.info("Stopping " + reader);

                stopped.add(reader.stop());
            }
            
            stopped.waitForAll();
            
            logger.info("All readers stopped");
            
        } finally {
            NDC.pop();
        }
    }

    private class TcpReader extends ActiveService {
        
        private final ReaderSignature signature;
        private boolean secure;

        private Socket socket;
        private BufferedReader br;

        public TcpReader(ReaderSignature signature) {
            
            this.signature = signature;
            secure = signature.secure;
        }

        public TemperatureSensor getInstance(String address) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void startup() throws Throwable {
            
            if (secure) {

                logger.info("Secure connection requested");

                try {

                    socket = SSLContextFactory.createContext(signature.password).getSocketFactory().createSocket(signature.remoteHost, signature.port);

                } catch (SSLException ex) {

                    logger.warn("Can't establish a secure connection to " + signature.remoteHost + ":" + signature.port, ex);
                    logger.warn("REVERTING TO INSECURE CONNECTION");
                    
                    secure = false;
                }
            }

            if ( socket == null ) {

                socket = new Socket(signature.remoteHost, signature.port);
            }

            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if ( !secure ) {

                // In case we're talking to a secure socket implementation,
                // we'll get stuck reading the input stream - they won't
                // tell us anything, nor would they break away (well, maybe
                // after a really long timeout). Therefore, we'll have to
                // give them a kick so they kick us out and we'll get a
                // visible indication of a failure.

                PrintWriter pw = new PrintWriter(socket.getOutputStream());

                pw.println("");
                pw.println("");
                pw.println("");
                pw.println("");
                pw.println("");
                pw.println("");
                pw.flush();

                // If they're secure, and we're not, we'll get kicked out
                // right here...
            }
        }

        final int RETRY_COUNT = 20;

        protected void execute() throws Throwable {

            int retryCount = RETRY_COUNT;

            while ( retryCount > 0 ) {

                while ( isEnabled() ) {

                    if (br == null) {

                        // This means that the connection reestablishment
                        // attempt has just failed
                        break;
                    }

                    try {
                        
                        if (!readLine()) {
                            break;
                        }

                    } catch (SSLException ex) {

                        if (ex.getMessage() != null) {

                            if (ex.getMessage().equals("Unrecognized SSL message, plaintext connection?")) {

                                // Let's retry as insecure

                                logger.warn("Can't establish secure connection to "
                                                               + signature.remoteHost + ":" + signature.port
                                                               + ", other end seems to be plaintext");
                                logger.warn("REVERTING TO INSECURE CONNECTION");

                                secure = false;
                                socket = null;
                                br = null;

                                break;
                            }

                        } else {

                            // I don't know what it is
                            throw ex;
                        }
                    }

                }

                if (!isEnabled()) {
                    
                    logger.info("Not enabled, terminating");
                    
                    assert(socket == null);

                    return;
                }

                // If we're here, it means that the socket has gone bad and
                // retryCount is positive. Let's try to reestablish the
                // connection.

                Thread.sleep(10000);

                try {

                    socket = new Socket(signature.remoteHost, signature.port);
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    retryCount = RETRY_COUNT;

                    logger.info("Connection reestablished");

                } catch (IOException ioex) {

                    logger.info("Can't reestablish the connection, "
                    	+ retryCount
                    	+ " retries left, cause:", ioex);

                    socket = null;
                    br = null;
                }

                retryCount--;
            }

            logger.fatal("Retry count exceeded, sensor server "
                    + signature.remoteHost + ":" + signature.port + " unavailable, terminating");
        }

        @Override
        protected void shutdown() throws Throwable {
            
            NDC.push("shutdown");
            
            try {
                
                if (socket != null) {
                    socket.close();
                }
                
            } finally {
                NDC.pop();
            }

        }

        private boolean readLine() throws IOException {
            
            NDC.push("readLine");
            
            try {
                
                long timestamp = System.currentTimeMillis();
                String line = br.readLine();

                if ( line == null ) {

                    logger.error("Socket broken, exiting loop");

                    processError(timestamp, "Connection Lost");
                    return false;
                }

                StringTokenizer st = new StringTokenizer(line, " ");

                // There is a few reserved words:
                //
                //      D       Data
                //      E       Error
                //      +       Arrival
                //      -       Departure
                //
                // If the first word (as in 'whitespace delimited
                // sequence') is reserved, then the parsing logic
                // changes. Otherwise, we process it just as before.

                String header = st.nextToken();

                if ( "E".equals(header) ) {

                    processError(timestamp, line.substring(2));

                } else if ( "+".equals(header) ) {

                    processArrival(timestamp, st.nextToken());

                } else if ( "-".equals(header) ) {

                    processDeparture(timestamp, st.nextToken());

                } else if ( "D".equals(header) ) {

                    processData(timestamp, line.substring(2));

                } else {

                    processData(timestamp, line);
                }
                
                return true;

            } finally {
                NDC.pop();
            }
        }

        private void processData(long timestamp, String line) {

            StringTokenizer st = new StringTokenizer(line, " ");

            String address = st.nextToken();
            String value = st.nextToken();

            TcpTemperatureSensor sensor = address2sensor.get(address);

            if ( sensor != null ) {

                sensor.consume(new DataSample<Double>(timestamp, address, "FIXME", Double.parseDouble(value), null));
            }
        }

        private void processError(long timestamp, String line) {

            // VT: FIXME: This may not be associated with a sensor address
            // (such as 1-Wire short circuit). Therefore, it may be
            // necessary to broadcast such a condition to all the sensors.

            // VT: NOTE: However, it may so happen that the error is indeed
            // related to an individual sensor, so let's try to process it
            // as usual.

            logger.warn("Error reported: " + line);

            StringTokenizer st = new StringTokenizer(line, " ");
            String address = st.nextToken();

            TcpTemperatureSensor s = address2sensor.get(address);

            if ( s == null ) {

                logger.warn("General error: " + line);
                
                for (Iterator<String> i = address2sensor.keySet().iterator(); i.hasNext(); ) {
                    
                    String sensorAddress = i.next();
                    TcpTemperatureSensor sensor = address2sensor.get(sensorAddress);
                    DataSample<Double> sample = new DataSample<Double>(timestamp, sensorAddress, "FIXME", null, new IOException(line));
                    
                    sensor.consume(sample);
                }

            } else {

                String error = line.substring(address.length() + 1);
                s.consume(new DataSample<Double>(timestamp, address, "FIXME", null, new IOException(error)));
            }
        }

        private void processArrival(long timestamp, String address) {

            // VT: NOTE: There's nothing to process, really. To the actual
            // consumers, the arrival itself doesn't mean anything at all
            // unless the sensor starts producing data - and this case is
            // already handled.

            logger.info("Sensor arrived: " + address);
        }

        private void processDeparture(long timestamp, String address) {

            logger.warn("Sensor departed: " + address);

            TcpTemperatureSensor sensor = address2sensor.get(address);

            if ( sensor != null ) {

                // VT: NOTE: Let the consumer worry about handling the
                // momentary lapses - the control logic doesn't belong at
                // this level
                DataSample<Double> sample = new DataSample<Double>(timestamp, address, "FIXME", null, new IOException("Sensor Departed"));
                sensor.consume(sample);
            }
        }
    }
    
    private static class ReaderSignature extends TcpConnectionSignature {
        
        public final String remoteHost;
        
        public ReaderSignature(String remoteHost, int port, boolean secure, String password) {
            
            super(port, secure, password);

            this.remoteHost = remoteHost;
        }

        protected void render(final StringBuilder sb) {
            
            sb.append("(").append(remoteHost).append(":").append(port);
            sb.append(secure ? ",secure" : "");
            sb.append(secure ? "," : "").append(secure ? password : "");
        }
    }
    
    private class TcpTemperatureSensor extends AbstractAnalogSensor implements DataSink<Double> {
        
        private DataSample<Double> lastKnownSignal;

        public TcpTemperatureSensor(String address, int pollIntervalMillis) {
            super(address, pollIntervalMillis);
            
            lastKnownSignal = new DataSample<Double>(System.currentTimeMillis(), address, "FIXME", null, new IllegalStateException("Not Available"));
        }

        @Override
        public DataSample<Double> getSensorSignal() throws IOException {
            
            return lastKnownSignal;
        }

        @Override
        protected void shutdown() throws Throwable {
            
            // They don't need us anymore
            address2sensor.remove(getAddress());
        }

        @Override
        protected void startup() throws Throwable {
            
            // Do nothing
        }

        public void consume(DataSample<Double> sample) {
            
            lastKnownSignal = sample;
        }
    }
}
