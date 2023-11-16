package net.sf.dz3r.runtime.config.model;

import net.sf.dz3r.instrumentation.InstrumentCluster;
import net.sf.dz3r.runtime.config.ConfigurationContext;
import net.sf.dz3r.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.view.webui.v2.WebUI;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class WebUiConfigurationParser extends ConfigurationContextAware {

    private final InstrumentCluster ic;

    public WebUiConfigurationParser(ConfigurationContext context, InstrumentCluster ic) {
        super(context);

        this.ic = ic;
    }

    public WebUI parse(WebUiConfig cf) {

        if (cf == null) {
            logger.warn("WebUI is not configured");
            return null;
        }

        var port = Optional.ofNullable(cf.port()).orElse(WebUI.DEFAULT_PORT);
        var interfaces = Optional.ofNullable(cf.interfaces()).orElse("0.0.0.0");
        var directors = context
                .directors
                .getFlux()
                .filter(d -> isConfigured("web-ui.directors", cf.directors(), d))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet())
                .block();

        var webUI = new WebUI(port, interfaces, context.endpoint.getFlux().blockFirst().getValue(), directors, ic, Optional.ofNullable(cf.units()).orElse(TemperatureUnit.C));

        // Needs to be resolvable to stop mDNS advertisements at the end
        context.webUI.register("web-ui", webUI);

        return webUI;
    }
}
