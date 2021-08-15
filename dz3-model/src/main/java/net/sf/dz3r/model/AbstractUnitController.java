package net.sf.dz3r.model;

public abstract class AbstractUnitController implements UnitController {

    private final String name;

    protected AbstractUnitController(String name) {
        this.name = name;
    }

    @Override
    public final String getAddress() {
        return name;
    }
}
