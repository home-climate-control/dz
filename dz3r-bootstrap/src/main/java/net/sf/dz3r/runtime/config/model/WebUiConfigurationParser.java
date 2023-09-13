package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.view.webui.v2.WebUI;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class WebUiConfigurationParser extends ConfigurationContextAware {

    public WebUiConfigurationParser(ConfigurationContext context, InstrumentCluster ic) {
        super(context);
    }

    public WebUI parse(WebUiConfig cf) {

        if (cf == null) {
            logger.warn("WebUI is not configured");
            return null;
        }

        var port = Optional.ofNullable(cf.port()).orElse(WebUI.DEFAULT_PORT);
        var directors = context
                .directors
                .getFlux()
                .filter(d -> isConfigured("web-ui.directors", cf.directors(), d))
                .map(Map.Entry::getValue)
                .map(Object.class::cast)
                .collect(Collectors.toSet())
                .block();

        return new WebUI(port, directors);
    }
}
