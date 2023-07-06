package net.sf.dz3.runtime.config.model;

import net.sf.dz3r.view.webui.v2.WebUI;

import java.util.Optional;
import java.util.Set;

public class WebUiConfigurationParser {

    public WebUI parse(WebUiConfig cf) {

        var port = Optional.ofNullable(cf.port()).orElse(WebUI.DEFAULT_PORT);

        return new WebUI(port, Set.of());
    }
}
