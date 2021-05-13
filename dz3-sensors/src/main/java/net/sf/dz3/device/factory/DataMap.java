package net.sf.dz3.device.factory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sf.dz3.device.sensor.DeviceFactory.Type;

/**
 * Data map.
 *
 * This class is introduced to improve reliability of transition between
 * the version of code where one physical device corresponds to one
 * logical device, and the version of code where one physical device may
 * correspond to several logical devices of different types (such as
 * DS2438 posing as both temperature and humidity container, or XBee based
 * devices which can act as practically anything).
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2004-2020
 */
public class DataMap {

    /**
     * Data map.
     *
     * The key is the device address, the value is a map with the key
     * being the device type ("temperature" or "humidity" at this point,
     * maybe some more later) and the value being last known device
     * reading.
     */
    private Map<String, Map<Type, Object>> dataMap = new TreeMap<String, Map<Type, Object>>();

    /**
     * Store the value.
     *
     * @param address Device address.
     *
     * @param type Device type.
     *
     * @param value Value associated to the device (last reading).
     */
    public void put(String address, Type type, Object value) {

        // Resolve the container map

        Map<Type, Object> containerMap = dataMap.get(address);

        if (containerMap == null) {

            containerMap = new TreeMap<Type, Object>();

            dataMap.put(address, containerMap);
        }

        // Store the value

        containerMap.put(type, value);
    }

    /**
     * Get the value.
     *
     * @param address Device address.
     *
     * @param type Device type ("temperature", "humidity", "switch").
     *
     * @return Value associated with the device address and type.
     */
    public Object get(String address, Type type) {

        // Resolve the container map

        Map<Type, Object> containerMap = dataMap.get(address);

        if ( containerMap == null ) {

            return null;
        }

        return containerMap.get(type);
    }

    /**
     * Transfer the data stored in this map into another map.
     *
     * @param target Data map to transfer data into.
     */
    public void transferTo(DataMap target) {

        for ( Iterator<String> i = dataMap.keySet().iterator(); i.hasNext(); ) {

            String address = i.next();
            Map<Type, Object> containerMap = dataMap.get(address);

            for ( Iterator<Type> ti = containerMap.keySet().iterator(); ti.hasNext(); ) {

                Type type = ti.next();
                Object value = containerMap.get(type);

                target.put(address, type, value);
            }
        }
    }

    @Override
    public String toString() {

        return dataMap.toString();
    }
}
