package net.sf.dz3.runtime;

public class NativeSensorDescriptor extends SensorDescriptor {

    public final String name;
    public final int type;

    public NativeSensorDescriptor(String name, int type) {

        this.name = name;
        this.type = type;
    }

    public String toString() {

        return name + "(type=" + type + ")";
    }
}
