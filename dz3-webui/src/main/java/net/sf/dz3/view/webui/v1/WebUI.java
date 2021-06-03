package net.sf.dz3.view.webui.v1;

import java.util.HashSet;
import java.util.Set;

/**
 * Web UI for Home Climate Control.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class WebUI {

    private final int port; // NOSONAR We'll get to it
    private final Set<Object> initSet = new HashSet<>(); // NOSONAR We'll get to it

    public WebUI(int port, Set<Object> initSet) {

        this.port = port;
        this.initSet.addAll(initSet);
    }
}
