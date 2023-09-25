package net.sf.dz3r.jmx;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2007-2009
 */
public class JmxDescriptor {
    public final String domainName;
    public final String name;
    public final String instance;
    public final String description;

    public JmxDescriptor(String domainName, String name, String instance, String description) {

        // Let the others worry about nulls
        this.domainName = domainName;
        this.name = name;
        this.instance = instance;
        this.description = description;
    }
}
