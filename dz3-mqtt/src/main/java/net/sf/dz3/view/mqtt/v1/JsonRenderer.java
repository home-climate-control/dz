package net.sf.dz3.view.mqtt.v1;

/**
 * JSON renderer abstraction.
 *  
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2010-2019
 */
public interface JsonRenderer<State> {
    
    /**
     * Render the given object as a JSON string.
     * 
     * @param source Object to render.
     * 
     * @return JSON string representing the source object.
     */
    String render(State source);
}
