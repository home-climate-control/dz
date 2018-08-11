package net.sf.dz3.device.factory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data map.
 *
 * <p>
 *
 * This class is introduced to improve reliability of transition between
 * the version of code where one physical device corresponds to one
 * logical device, and the version of code where one physical device may
 * correspond to several logical devices of different types (such as
 * DS2438 posing as both temperature and humidity container, or XBee based
 * devices which can act as practically anything).
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2004-2010
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
    private Map<String, Map<String, Object>> dataMap = new TreeMap<String, Map<String, Object>>();

    /**
     * Store the value.
     *
     * @param address Device address.
     *
     * @param type Device type ("temperature", "humidity", "switch").
     *
     * @param value Value associated to the device (last reading).
     */
    public void put(String address, String type, Object value) {

        checkType(type);

        // Resolve the container map

        Map<String, Object> containerMap = dataMap.get(address);

        if (containerMap == null) {

            containerMap = new TreeMap<String, Object>();

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
    public Object get(String address, String type) {

        checkType(type);

        // Resolve the container map

        Map<String, Object> containerMap = dataMap.get(address);

        if ( containerMap == null ) {

            return null;
        }

        return containerMap.get(type);
    }

    /**
     * @param type Type to check.
     *
     * @exception IllegalArgumentException if the type is not one of
     * "temperature", "humidity", "switch".
     */
    private void checkType(String type) {

        if (    "switch".equals(type)
                || "temperature".equals(type)
                || "humidity".equals(type) ) {

            return;
        }

        throw new IllegalArgumentException("Invalid type '" + type + "', only 'temperature', 'switch' and 'humidity' are allowed");
    }

    /**
     * Transfer the data stored in this map into another map.
     *
     * @param target Data map to transfer data into.
     */
    public void transferTo(DataMap target) {

        for ( Iterator<String> i = dataMap.keySet().iterator(); i.hasNext(); ) {

            String address = i.next();
            Map<String, Object> containerMap = dataMap.get(address);

            for ( Iterator<String> ti = containerMap.keySet().iterator(); ti.hasNext(); ) {

                String type = ti.next();
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
