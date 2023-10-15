package net.sf.dz3r.runtime.config;

import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.device.mqtt.v1.MqttAdapter;
import net.sf.dz3r.model.UnitController;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.runtime.config.onewire.EntityProvider;
import net.sf.dz3r.scheduler.ScheduleUpdater;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.view.Connector;
import net.sf.dz3r.view.MetricsCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

/**
 * Common space for all the objects emitted and consumed by configurators.
 *
 * Some components (notably MQTT, 1-Wire, XBee, GAE and Calendar) are heavy on startup, and can yield transient errors
 * long after their fluxes can be resolved, hence, all components are exposed as fluxes, for uniformity and just-in-time delivery.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public class ConfigurationContext {

    protected final Logger logger = LogManager.getLogger();

    public final EntityProvider<MqttAdapter> mqtt = new EntityProvider<>("mqtt");
    public final EntityProvider<Flux<Signal<Double, Void>>> sensors = new EntityProvider<>("sensor");
    public final EntityProvider<Switch<?>> switches = new EntityProvider<>("switch");
    public final EntityProvider<Zone> zones = new EntityProvider<>("zone");
    public final EntityProvider<ScheduleUpdater> schedule = new EntityProvider<>("schedule");
    public final EntityProvider<Connector> connectors = new EntityProvider<>("connector");
    public final EntityProvider<MetricsCollector> collectors = new EntityProvider<>("collector");
    public final EntityProvider<HvacDevice> hvacDevices = new EntityProvider<>("HVAC device");
    public final EntityProvider<UnitController> units = new EntityProvider<>("unit controller");
    public final EntityProvider<UnitDirector> directors = new EntityProvider<>("unit director");
}
