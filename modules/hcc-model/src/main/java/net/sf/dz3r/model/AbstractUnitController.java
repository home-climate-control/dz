package net.sf.dz3r.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractUnitController implements UnitController {

    protected final Logger logger = LogManager.getLogger();
    private final String name;

    protected AbstractUnitController(String name) {
        this.name = name;
    }

    @Override
    public final String getAddress() {
        return name;
    }
}
