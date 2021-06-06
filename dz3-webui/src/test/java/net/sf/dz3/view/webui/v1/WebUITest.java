package net.sf.dz3.view.webui.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import net.sf.dz3.device.sensor.AnalogSensor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebUITest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sensorSample() throws JsonProcessingException {

        var sensor = mock(AnalogSensor.class);

        when(sensor.getAddress()).thenReturn("address");
        when(sensor.getSignal()).thenReturn(new DataSample<>(0L,"source", "signature", 0.1d, null));

        var snapshot = new AnalogSensorSnapshot(sensor);

        var json = objectMapper.writeValueAsString(snapshot);

        assertThat(json).isEqualTo("{\"address\":\"address\",\"signal\":{\"timestamp\":0,\"sourceName\":\"source\",\"signature\":\"signature\",\"sample\":0.1,\"error\":false}}");
    }

    @Test
    void sensorError() throws JsonProcessingException {

        var sensor = mock(AnalogSensor.class);

        when(sensor.getAddress()).thenReturn("address");
        when(sensor.getSignal()).thenReturn(new DataSample<>(0L,"source", "signature", null, new Error("stale (last sample 15 min ago)")));

        var snapshot = new AnalogSensorSnapshot(sensor);

        var json = objectMapper.writeValueAsString(snapshot);

        assertThat(json).isEqualTo("{\"address\":\"address\",\"signal\":{\"timestamp\":0,\"sourceName\":\"source\",\"signature\":\"signature\",\"sample\":null,\"error\":true}}");
    }
}
