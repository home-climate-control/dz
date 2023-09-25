package net.sf.dz3r.device.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;

public class OneWireEndpoint {
    /**
     * 1-Wire adapter port name.
     */
    public final String port;

    /**
     * Adapter speed.
     *
     * This value is injected via constructor. If the value given is bad, it will be
     * defaulted to {@link DSPortAdapter.Speed#REGULAR}.
     */
    public final DSPortAdapter.Speed speed;

    public OneWireEndpoint(String port, DSPortAdapter.Speed speed) {
        this.port = port;
        this.speed = speed;
    }

    @Override
    public String toString() {
        return "{1-Wire port=" + port + ", speed=" + speed + "}";
    }
}
