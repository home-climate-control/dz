package net.sf.dz3r.view.http.v3;

import net.sf.dz3r.view.Connector;

import java.net.URL;

public abstract class HttpConnector implements Connector {

    protected final URL serverContextRoot;

    protected HttpConnector(URL serverContextRoot) {
        this.serverContextRoot = serverContextRoot;
    }
}
