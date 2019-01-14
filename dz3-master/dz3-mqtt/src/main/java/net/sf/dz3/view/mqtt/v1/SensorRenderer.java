package net.sf.dz3.view.mqtt.v1;

import java.io.StringWriter;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(UpstreamBlock) stuffing} them into the queue. 
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public class SensorRenderer extends QueueFeeder<UpstreamBlock> implements DataSink<Double>, JsonRenderer<DataSample<Double>> {

    private final AnalogSensor source;

    public SensorRenderer(AnalogSensor source, Map<String, Object> context) {

        super(context);

        this.source = source;

        source.addConsumer(this);
    }

    /**
     * VT: FIXME: Create {@code MqttRenderer}
     */
    private String getTopic() {
        return "sensor/" + source.getAddress();
    }

    @Override
    public void consume(DataSample<Double> signal) {
        emit(new UpstreamBlock(getTopic(), render(signal)));
    }

    /**
     * Render a simple JSON representation.
     */
    @Override
    public String render(DataSample<Double> source) {

        JsonObjectBuilder b = Json.createObjectBuilder();

        b.add("timestamp", source.timestamp);
        b.add("name", source.sourceName);
        b.add("signature", source.signature);

        if (!source.isError()) {

            b.add("signal", source.sample);

        } else {

            b.add("error", source.error.getMessage());
        }

        JsonObject message = b.build();
        StringWriter sw = new StringWriter();
        JsonWriter jw = Json.createWriter(sw);

        jw.writeObject(message);
        jw.close();

        return sw.toString();
    }
}
