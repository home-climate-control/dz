package net.sf.dz3.device.model.impl;

import net.sf.dz3.device.model.HvacMode;

import java.io.Serializable;

public class HvacDriverSignal  implements Serializable {

    private static final long serialVersionUID = 323387681093517558L;

    public final HvacMode[] mode;
    public final int[] stage;
    public final double[] fanSpeed;

    public HvacDriverSignal(
            HvacMode[] mode,
            int[] stage,
            double[] fanSpeed) {

        this.mode = mode;
        this.stage = stage;
        this.fanSpeed = fanSpeed;
    }
}
