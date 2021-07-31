package net.sf.dz3.device.sensor.impl;

import com.homeclimatecontrol.jukebox.datastream.logger.impl.DataBroadcaster;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import net.sf.dz3.device.sensor.Addressable;
import net.sf.dz3.device.sensor.AnalogConverter;
import net.sf.dz3.device.sensor.AnalogSensor;

import java.util.StringTokenizer;

/**
 * Analog passthrough reading the data from the source sensor and
 * passing it down after converting the sample.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2010-2012
 */
public final class ConvertingSensor implements AnalogSensor, DataSink<Double> {

    private final AnalogSensor source;
    private final AnalogConverter converter;
    private double calibrationShift = 0d;

    private final DataBroadcaster<Double> dataBroadcaster = new DataBroadcaster<>();

    /**
     * Create an instance without calibration.
     *
     * @param source Signal source.
     * @param converter Converter instance. Can't be {@code null}.
     */
    public ConvertingSensor(AnalogSensor source, AnalogConverter converter) {

        this(source, converter, 0d);

        if (converter == null) {
            throw new IllegalArgumentException("converter can't be null - what's the point???");
        }
    }

    /**
     * Create calibrated passthrough instance.
     *
     * @param source Signal source.
     * @param calibrationShift Calibration shift, {@code 0} if no calibration is required.
     */
    public ConvertingSensor(AnalogSensor source, double calibrationShift) {

        this(source, null, calibrationShift);
    }

    /**
     * Create calibrated converted instance.
     *
     * @param source Signal source.
     * @param converter Converter instance. Can be {@code null} if only calibration is required.
     * @param calibrationShift Calibration shift, {@code 0} if no calibration is required.
     */
    public ConvertingSensor(AnalogSensor source, AnalogConverter converter, double calibrationShift) {

        if (source == null) {
            throw new IllegalArgumentException("source can't be null");
        }

        this.source = source;
        this.converter = converter;
        this.calibrationShift = calibrationShift;

        source.addConsumer(this);
    }

    private DataSample<Double> convert(DataSample<Double> signal) {

        if (signal == null) {

            return new DataSample<>(System.currentTimeMillis(), source.getAddress(), source.getAddress(), null, new IllegalStateException("signal is null"));
        }

        Double sample;

        sample = converter != null ? converter.convert(signal.sample) : signal.sample;
        sample = sample != null ? sample + calibrationShift : null;

        return new DataSample<>(signal.timestamp, signal.sourceName, signal.signature, sample, signal.error);
    }

    @Override
    public DataSample<Double> getSignal() {

        return convert(source.getSignal());
    }

    @Override
    public void addConsumer(DataSink<Double> consumer) {

        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public void removeConsumer(DataSink<Double> consumer) {

        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        String rawAddress = getAddress();

        StringBuilder sb = new StringBuilder();

        for (StringTokenizer st = new StringTokenizer(rawAddress, ":"); st.hasMoreTokens(); ) {

            sb.append(st.nextToken());

            if (st.hasMoreTokens()) {

                sb.append(" channel ");
            }
        }

        var address = sb.toString();

        return new JmxDescriptor(
                "dz",
                getClass().getSimpleName(),
                address,
                "Applying " + converter.getClass().getSimpleName() + " conversion to " + source);
    }

    @Override
    public String getAddress() {
        return source.getAddress();
    }

    @Override
    public void consume(DataSample<Double> signal) {
        dataBroadcaster.broadcast(convert(signal));
    }

    /**
     * Get the difference between the converted sensor reading and output signal.
     *
     * @return Calibration shift.
     */
    @JmxAttribute(description = "Calibration shift")
    public Double getCalibrationShift() {
        return calibrationShift;
    }

    /**
     * Set the calibration shift.
     *
     * @param calibrationShift Calibration shift to set.
     *
     * @see #convert(DataSample)
     */
    public void setCalibrationShift(Double calibrationShift) {

        this.calibrationShift = calibrationShift;
    }

    @Override
    public int compareTo(Addressable o) {
        // Can't afford to collide with the wrapper
        return (getClass().getName() + getAddress()).compareTo((o.getClass().getName() + o.getAddress()));
    }
}
