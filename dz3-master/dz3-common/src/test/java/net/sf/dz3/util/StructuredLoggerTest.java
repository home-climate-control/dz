package net.sf.dz3.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Test;

public class StructuredLoggerTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testIdMsgType() {
        ThreadContext.push("testIdMsgType");
        try {
            // RFC 5424 formatted message
            logger.info(new StructuredDataMessage("id", "msg", "type"));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testStructuredIdMsgType() {
        ThreadContext.push("testIdMsgType");
        try {
            // RFC 5424 formatted message
            logger.info(new StructuredDataMessage(new StructuredDataId("id", 1, new String[] {"huh"}, null), "msg", "type"));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testIdMsgTypeData() {
        ThreadContext.push("testIdMsgTypeData");
        try {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key 2", "value 2");
            // RFC 5424 formatted message
            logger.info(new StructuredDataMessage("id", "msg", "type", data));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testStructuredIdMsgTypeData() {
        ThreadContext.push("testStructuredIdMsgTypeData");
        try {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key 2", "value 2");
            // RFC 5424 formatted message
            logger.info(new StructuredDataMessage(new StructuredDataId("id", 1, new String[] {"huh"}, null), "msg", "type", data));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testIdMsgTypeDataQuoted() {
        ThreadContext.push("testIdMsgTypeDataQuoted");
        try {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "'value1'");
            data.put("key 2", "\"value 2\"");
            // RFC 5424 formatted message
            logger.info(new StructuredDataMessage("id", "msg", "type", data));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testObjectMessage() {
        ThreadContext.push("testObjectMessage");
        try {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key 2", "value 2");
            // Almost JSON formatted message - values are not quoted, this violates JSON specs
            logger.info(new ObjectMessage(data));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testObjectMessageException1() {
        ThreadContext.push("testObjectMessageException1");
        try {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key2", "value2");
            // This one won't pack the exception into the same message
            logger.info(new ObjectMessage(data), new Exception("Oops"));
        } finally {
            ThreadContext.pop();
        }
    }

    @Test
    public void testObjectMessageException2() {
        ThreadContext.push("testObjectMessageException2");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key2", "value2");
            data.put("exception", new Exception("Oops"));
            // VT: This one won't render the exception trace
            logger.info(new ObjectMessage(data));
        } finally {
            ThreadContext.pop();
        }
    }
}
