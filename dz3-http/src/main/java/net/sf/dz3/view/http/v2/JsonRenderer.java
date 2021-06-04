package net.sf.dz3.view.http.v2;

/**
 * JSON renderer abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2010-2021
 */
public interface JsonRenderer {

    /**
     * Render the given object as a JSON string.
     *
     * @param source Object to render.
     *
     * @return JSON string representing the source object.
     */
    String render(Object source);
}
