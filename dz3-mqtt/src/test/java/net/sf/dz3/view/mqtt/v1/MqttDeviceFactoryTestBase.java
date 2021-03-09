package net.sf.dz3.view.mqtt.v1;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MqttDeviceFactoryTestBase {

    protected final Logger logger = LogManager.getLogger(getClass());

    protected static final Random rg = new Random();
    protected static MqttDeviceFactory mdf;
    protected static String pubTopic;
    protected static String subTopic;


}
