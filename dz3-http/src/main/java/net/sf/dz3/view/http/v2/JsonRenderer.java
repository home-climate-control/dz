package net.sf.dz3.view.http.v2;

/**
 * JSON renderer abstraction.
 *  
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2010
 */
public interface JsonRenderer<State> {
    
    /**
     * Render the given object as a JSON string.
     * 
     * @param source Object to render.
     * 
     * @return JSON string representing the source object.
     */
    String render(Object source);
}
