package net.sf.dz3r.view.webui.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.sf.dz3r.model.UnitDirector;
import net.sf.dz3r.view.UnitObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Base class for HCC remote control endpoints.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2024
 */
public class Endpoint {

    protected final Logger logger = LogManager.getLogger();
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final Map<UnitDirector, UnitObserver> unit2observer;

    public Endpoint(Map<UnitDirector, UnitObserver> unit2observer) {
        this.unit2observer = unit2observer;
        objectMapper.registerModule(new JavaTimeModule());
    }
}
