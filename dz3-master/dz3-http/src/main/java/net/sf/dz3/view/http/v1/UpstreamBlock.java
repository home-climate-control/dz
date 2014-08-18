package net.sf.dz3.view.http.v1;

import java.util.Map;

/**
 * Block of information to send to HTTP server.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class UpstreamBlock {

    /**
     * Path relative to the root path.
     */
    public final String path;
    
    /**
     * PUT method arguments representing the object state.
     */
    public final Map<String, String> stateMap;
    
    /**
     * Create an instance.
     * 
     * @param path Path relative to the root path.
     * @param stateMap PUT method arguments representing the object state.
     */
    public UpstreamBlock(String path, Map<String, String> stateMap) {
        
        this.path = path;
        this.stateMap = stateMap;
    }
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("path=").append(path).append(", ");
        sb.append(stateMap);
        
        return sb.toString();
    }
}
