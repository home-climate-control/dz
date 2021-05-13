package net.sf.dz3.device.sensor.impl.tcp.server;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public interface ListenerMBean {

  String getHost();
  boolean isSecureRequested();
  boolean isSecure();
}
