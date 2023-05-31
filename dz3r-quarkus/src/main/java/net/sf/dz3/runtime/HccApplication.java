package net.sf.dz3.runtime;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

@ApplicationScoped
public class HccApplication {
    private final Logger logger = LogManager.getLogger();
    void onStart(@Observes StartupEvent e) {

        ThreadContext.push("onStart");
        try {
            logger.warn("Starting up");

            // VT: FIXME: Nothing to do now, need to materialize the app first, stay tuned

        } finally {
            ThreadContext.pop();
        }
    }

    @GET
    @Path("/hello")
    @Produces("text/plain")
    public String hello() {
        return "Oh, hai";
    }
}
