package net.sf.dz3.view.http.v1;

import java.util.Map;

/**
 * Abstraction for representing the state of an object in a form suitable for REST
 * (Representational State Transfer) exchange.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2010
 */
public interface RestRenderer<State> {

    /**
     * @return The object path.
     */
    String getPath();
    
    /**
     * @param state State to render.
     * 
     * @return Object state as a map.
     */
    Map<String, String> getState(State state);
}
