package com.homeclimatecontrol.hcc.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeclimatecontrol.hcc.client.http.HttpClient;
import com.homeclimatecontrol.hcc.client.rsocket.RSocketClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import reactor.tools.agent.ReactorDebugAgent;

import java.io.IOException;
import java.net.URL;

@SpringBootApplication
public class HccCLI implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(HccCLI.class);

    private static final String COMMAND_MDNS_SCAN = "mdns-scan";
    private static final String COMMAND_GET_META = "get-meta";
    private static final String COMMAND_GET_ZONES = "get-zones";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient();
    private final RSocketClient rsocketClient = new RSocketClient();

    public abstract class CommandBase {

        @Parameter(names = { "--url" }, description = "HCC meta URL to connect to", required = true)
        String url;
    }

    public class CommandGetMeta extends CommandBase {

    }

    public abstract class CommandRSocket extends CommandBase {

        @Parameter(names = { "--serialization" }, description = "Serialization method")
        String serialization = "JSON";
    }

    public class CommandGetZones extends CommandRSocket {

    }

    public class CommandMdnsScan {

        @Parameter(names = { "--timeout" }, description = "Scan timeout in seconds")
        int timeout = 1;
    }

    public static void main(String[] args) {

        ReactorDebugAgent.init();

        var builder = new SpringApplicationBuilder(HccCLI.class);

        builder.headless(false);

        var context = builder.run(args);

        context.close();
    }

    @Override
    public void run(String... args) throws Exception {

        var commandMdnsScan = new CommandMdnsScan();
        var commandGetMeta = new CommandGetMeta();
        var commandGetZones = new CommandGetZones();

        var jc = JCommander
                .newBuilder()
                .addCommand(COMMAND_MDNS_SCAN, commandMdnsScan)
                .addCommand(COMMAND_GET_META, commandGetMeta)
                .addCommand(COMMAND_GET_ZONES, commandGetZones)
                .build();

        try {

            jc.parse(args);

            if (jc.getParsedCommand() == null) {
                terminate(jc, "Please specify exactly one command", null);
            }

            switch (jc.getParsedCommand()) {

                case COMMAND_MDNS_SCAN -> mdnsScan();
                case COMMAND_GET_META -> getMeta(commandGetMeta.url);
                case COMMAND_GET_ZONES -> getZones(commandGetZones.url, commandGetZones.serialization);

            }

        } catch (ParameterException ex) {

            terminate(jc, "Invalid command line arguments", ex);

        } catch (Throwable t) {

            terminate(jc, "Unhandled exception, terminating",  t);
        }
    }

    private void mdnsScan() {
        ThreadContext.push("mdnsScan");
        try {
            throw new UnsupportedOperationException("Stay tuned");
        } finally {
            ThreadContext.pop();
        }
    }

    private void getMeta(String url) throws IOException {
        ThreadContext.push("getMeta");
        try {
            logger.info("url={}", url);
            var meta = httpClient.getMeta(new URL(url));
            var metaPrint = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta);
            logger.info("META/parsed: {}", metaPrint);

        } finally {
            ThreadContext.pop();
        }
    }

    private void getZones(String url, String serialization) throws IOException {
        ThreadContext.push("getZones");
        try {
            logger.info("url={}", url);

            var httpUrl = new URL(url);
            // First need to get this to determine the host and port to connect RSocket to
            var meta = httpClient.getMeta(httpUrl);
            var zoneMap = rsocketClient.getZones(httpUrl.getHost(), meta.instance().duplexPort(), serialization);

            logger.info("ZONES:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(zoneMap));

        } finally {
            ThreadContext.pop();
        }
    }

    private static void terminate(JCommander jc, String message, Throwable t) {

        logger.fatal(message,  t);
        jc.usage();
        System.exit(-1);
    }
}
