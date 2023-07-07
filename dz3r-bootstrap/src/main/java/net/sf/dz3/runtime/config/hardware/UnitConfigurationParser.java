package net.sf.dz3.runtime.config.hardware;

import net.sf.dz3.runtime.config.ConfigurationContext;
import net.sf.dz3.runtime.config.ConfigurationContextAware;
import net.sf.dz3r.model.MultistageUnitController;
import net.sf.dz3r.model.SingleStageUnitController;
import net.sf.dz3r.model.UnitController;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.Set;

public class UnitConfigurationParser extends ConfigurationContextAware {

    public UnitConfigurationParser(ConfigurationContext context) {
        super(context);
    }

    public void parse(Set<UnitControllerConfig> source) {

        for (var entry : source) {
            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.singleStage())
                            .orElse(Set.of()))
                    .map(this::parseSingleStage)
                    .blockLast();

            Flux
                    .fromIterable(Optional
                            .ofNullable(entry.multiStage())
                            .orElse(Set.of()))
                    .map(this::parseMultiStage)
                    .blockLast();
        }
    }

    private UnitController parseSingleStage(SingleStageUnitControllerConfig cf) {
        return new SingleStageUnitController(cf.id());
    }

    private UnitController parseMultiStage(MultiStageUnitControllerConfig cf) {
        return new MultistageUnitController(cf.id(), cf.stages());
    }
}
