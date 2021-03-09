package net.sf.dz3.view.influxdb.v1;

import java.util.Map;
import java.util.Set;

import net.sf.dz3.view.Connector;
import net.sf.dz3.view.ConnectorFactory;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * InfluxDB data source.
 * 
 * This object is supposed to be instantiated via Spring configuration file, with objects
 * that are supposed to be rendered being present in a set passed to the constructor.
 *
 * See {@code net.sf.dz3.view.swing.Console} for more information.
 *
 * {@code init-method="start"} attribute must be used in Spring bean definition, otherwise
 * the connector will not work.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public class InfluxDbConnector extends Connector<InfluxDbRenderer> {

    public InfluxDbConnector(Set<Object> initSet) {
        this(initSet, null);
    }

    public InfluxDbConnector(Set<Object> initSet, Set<ConnectorFactory<InfluxDbRenderer>> factorySet) {
        super(initSet, factorySet);
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected Map<String, Object> createContext() {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    protected void activate2() {

        try {

            throw new IllegalStateException("Not Implemented");

        } catch (Throwable t) {

            throw new IllegalStateException("failed to start", t);
        }
    }

    @Override
    protected void deactivate2() {
        throw new IllegalStateException("Not Implemented");
    }
}
