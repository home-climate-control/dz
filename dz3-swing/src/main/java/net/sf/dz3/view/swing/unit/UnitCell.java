package net.sf.dz3.view.swing.unit;

import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.device.model.UnitRuntimePredictionSignal;
import net.sf.dz3.view.swing.EntityCell;

public class UnitCell extends EntityCell<UnitRuntimePredictionSignal> {

    public UnitCell(RuntimePredictor source) {
        super(source);
    }
}
