package net.sf.dz3.device.sensor.impl.tcp.server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.dz3.device.sensor.TemperatureSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

public class SensorServer extends AbstractListener {

    public SensorServer(Set<String> addressSet, int port, int broadcastPort) {
        super(addressSet, port, broadcastPort);
    }

    public SensorServer(Set<String> addressSet, int port, int broadcastPort,
            boolean secure, String password) {
        super(addressSet, port, broadcastPort, secure, password);
    }

    @Override
    protected void cleanup() throws Throwable {

    }

    @Override
    protected ConnectionHandler createHandler(Socket socket, BufferedReader br, PrintWriter pw) {
        
        return new BroadcastHandler(socket, br, pw);
    }

    @Override
    public int getDefaultBroadcastPort() {
        
        return 5001;
    }

    @Override
    public int getDefaultListenPort() {
        
        return 5000;
    }

    @Override
    public String getServiceSignature() {
        
        return "DZ DAC Sensors";
    }

    @Override
    protected boolean isUnique() {
        
        return false;
    }

    @Override
    protected void shutdown2() throws Throwable {

    }

    @Override
    protected void startup2() throws Throwable {

    }

    public void stateChanged(TemperatureSensor source, DataSample<Double> snapshot) {
        
        
    }

    public class BroadcastHandler extends ConnectionHandler implements DataSink<Double> {
        
        /**
         * The key is the source name, the value is last known signal from this source.
         */
    	private final Map<String, DataSample<Double>> sensor2signal = new TreeMap<String, DataSample<Double>>(); 

        public BroadcastHandler(Socket socket, BufferedReader br, PrintWriter pw) {
            super(socket, br, pw);
        }

        @Override
        protected CommandParser createParser() {
            return null;
        }

        @Override
        protected void sayHello() {
            
            synchronized (sensor2signal) {
                iHave();
                
                for (Iterator<String> i = sensor2signal.keySet().iterator(); i.hasNext(); ) {
                    
                    String sensor = i.next();
                    DataSample<Double> signal = sensor2signal.get(sensor);
                    
                    send(renderSignal(signal));
                }
            }
        }

        @Override
        public void iHave() {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("IHAVE ").append(sensor2signal.size()).append(":");
            
            for (Iterator<String> i = sensor2signal.keySet().iterator(); i.hasNext(); ) {
                
                if (i.hasNext()) {
                    sb.append(" ");
                }
                
                sb.append(i.next());
            }
            
            send(sb.toString());
        }

		@Override
		public void consume(DataSample<Double> signal) {

            sensor2signal.put(signal.sourceName, signal);
            
			send(renderSignal(signal));
		}

		private String renderSignal(DataSample<Double> signal) {
            
            StringBuilder sb = new StringBuilder();
            
            sb.append(signal.isError() ? "E" : "D").append(" ").append(signal.sourceName);
            sb.append(signal.isError() ? signal.error.getMessage() : Double.toString(signal.sample));
            
            return sb.toString();
        }
    }
}
