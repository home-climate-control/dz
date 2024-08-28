package com.homeclimatecontrol.hcc.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class HccCLI implements CommandLineRunner {

    private static final Logger logger = LogManager.getLogger(HccCLI.class);

    private static final String COMMAND_GET_META = "get-meta";

    public abstract class CommandBase {

        @Parameter(names = { "--url" }, description = "URL to connect to", required = true)
        String url;
    }

    public class CommandGetMeta extends CommandBase {

    }

    public static void main(String[] args) {

        var builder = new SpringApplicationBuilder(HccCLI.class);

        builder.headless(false);

        var context = builder.run(args);

        context.close();
    }

    @Override
    public void run(String... args) throws Exception {

        var commandGetMeta = new CommandGetMeta();
        var jc = JCommander
                .newBuilder()
                .addCommand(COMMAND_GET_META, commandGetMeta)
                .build();

        try {

            jc.parse(args);

            if (jc.getParsedCommand() == null) {
                terminate(jc, "Please specify exactly one command", null);
            }

            switch (jc.getParsedCommand()) {

                case COMMAND_GET_META -> getMeta(commandGetMeta.url);

            }

        } catch (ParameterException ex) {

            terminate(jc, "Invalid command line arguments", ex);

        } catch (Throwable t) {

            terminate(jc, "Unhandled exception, terminating",  t);
        }
    }

    private void getMeta(String url) {
        ThreadContext.push("getMeta");
        try {
            logger.info("url={}", url);
            throw new UnsupportedOperationException("Stay tuned");
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
