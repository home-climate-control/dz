package net.sf.dz3r.device.actuator.damper;

import com.homeclimatecontrol.hcc.model.ZoneSettings;
import com.homeclimatecontrol.hcc.signal.hvac.CallingStatus;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.instrumentation.Marker;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.UnitControlSignal;
import net.sf.dz3r.signal.hvac.ZoneStatus;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Set of test cases to replicate https://github.com/home-climate-control/dz/issues/172.
 */
@Disabled("Timing is precarious, will fail CI/CD unless made annoyingly long")
class THTest {

    private final Logger logger = LogManager.getLogger();
    private final Random rg = new SecureRandom();

    private static final long SETTLEMENT_DELAY_MILLIS = 40;

    @Test
    void testSyncFastSimple()
            throws IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {

        testSync("fast/simple", SimpleDamperController.class, 0, 0);
    }

    @Test
    void testSyncSlowSimple()
            throws IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {

        testSync("slow/simple", SimpleDamperController.class, 1, 5);
    }

    @Test
    void testSyncFastBalancing()
            throws IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {

        testSync("fast/balancing", BalancingDamperController.class, 0, 0);
    }

    @Test
    void testSyncSlowBalancing()
            throws IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {

        testSync("slow/balancing", BalancingDamperController.class, 1, 5);
    }

    private void testSync(String marker, Class<? extends AbstractDamperController> controllerClass, long minDelay, int maxDelay)
            throws IOException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InterruptedException {

        var m = new Marker(marker);
        ThreadContext.push(marker);

        try {

            var tsLivingRoom = mock(Zone.class);
            var tsKitchen = mock(Zone.class);
            var tsWestBathroom = mock(Zone.class);
            var tsWest = mock(Zone.class);

            doReturn("thermostat-livingroom").when(tsLivingRoom).getAddress();
            doReturn("thermostat-kitchen").when(tsKitchen).getAddress();
            doReturn("thermostat-west_bathroom").when(tsWestBathroom).getAddress();
            doReturn("thermostat-west").when(tsWest).getAddress();

            Scheduler switchScheduler = Schedulers.newSingle("switch scheduler", true);

            var switchLivingRoom = new NullSwitch("switch_livingroom_damper", false, minDelay, maxDelay, switchScheduler);
            var switchKitchen = new NullSwitch("switch_kitchen_damper", false, minDelay, maxDelay, switchScheduler);
            var switchWestBathroom = new NullSwitch("switch_west_bathroom_damper", false, minDelay, maxDelay, switchScheduler);
            var switchWestDamper = new NullSwitch("switch_west_damper", false, minDelay, maxDelay, switchScheduler);
            var switchWestBoosterFan = new NullSwitch("switch_west_boosterfan", false, minDelay, maxDelay, switchScheduler);

            var damperLivingRoom = new SwitchDamper<>("damper_livingroom", switchLivingRoom, 0.8, 1.0);
            var damperKitchen = new SwitchDamper<>("damper_kitchen", switchKitchen, 0.8, 1.0);
            var damperWestBathroom = new SwitchDamper<>("damper_west_bathroom", switchWestBathroom, 0.8, 1.0);

            var damperWest = new SwitchDamper<>("damper_west", switchWestDamper, 0.8, 1.0);
            var damperWestBoosterFan = new SwitchDamper<>("damper_west_boosterfan", switchWestBoosterFan, 0.8, 0.0, true);

            var damperMultiplexerWest = new DamperMultiplexer<>("damper_multiplexer_west", Set.of(damperWest, damperWestBoosterFan));

            // TreeMap will not work here because of the way Mockito works
            var zone2damper = new LinkedHashMap<Zone, Damper<?>>();

            zone2damper.put(tsLivingRoom, damperLivingRoom);
            zone2damper.put(tsKitchen, damperKitchen);
            zone2damper.put(tsWestBathroom, damperWestBathroom);
            zone2damper.put(tsWest, damperMultiplexerWest);

            Constructor<? extends AbstractDamperController> c = controllerClass.getDeclaredConstructor(Map.class);
            var dc = c.newInstance(zone2damper);

            var unitQueue = new LinkedBlockingQueue<Signal<UnitControlSignal, Void>>();
            var zoneQueue = new LinkedBlockingQueue<Signal<ZoneStatus, String>>();

            Flux<Signal<UnitControlSignal, Void>> unitFlux = Flux.create(sink -> {
                try {
                    var item = unitQueue.take();

                    if (item.isError()) {
                        sink.complete();
                        return;
                    }
                    sink.next(item);
                } catch (InterruptedException ex) {
                    sink.error(ex);
                }
            });

            Flux<Signal<ZoneStatus, String>> zoneFlux = Flux.create(sink -> {
                try {
                    var item = zoneQueue.take();

                    if (item.isError()) {
                        sink.complete();
                        return;
                    }
                    sink.next(item);
                } catch (InterruptedException ex) {
                    sink.error(ex);
                }
            });

            var timestamp = Instant.now().toEpochMilli();

            Flux<Pair<Signal<UnitControlSignal, Void>, Signal<ZoneStatus, String>>> initialState = Flux.just(
                    new ImmutablePair<>(
                            new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(0.0, 0.0)),
                            null)
            );

            new Thread(() -> {
                dc.compute(unitFlux.doOnNext(s -> logger.info("unitFlux: {}", s)), zoneFlux.doOnNext(s -> logger.info("zoneFlux: {}", s)))
                        .doOnNext(item -> logger.warn("dc: {}", item))
                        .doOnComplete(() -> logger.warn("dc: complete"))
                        .blockLast();
            }).start();

            execute(initialState, unitQueue, zoneQueue);
            Thread.sleep(SETTLEMENT_DELAY_MILLIS);

            // The unit is off, dampers are parked

            assertThat(switchLivingRoom.getState().block()).isTrue();
            assertThat(switchKitchen.getState().block()).isTrue();
            assertThat(switchWestBathroom.getState().block()).isTrue();
            assertThat(switchWestDamper.getState().block()).isTrue();
            assertThat(switchWestBoosterFan.getState().block()).isTrue();

            var zoneSettings = new ZoneSettings(20.0);

            // For a good measure, let's advance the timestamp between signals
            timestamp += 50 + rg.nextInt(100);

            Flux<Pair<Signal<UnitControlSignal, Void>, Signal<ZoneStatus, String>>> step1 = Flux.just(
                    new ImmutablePair<>(
                            new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(3.0625, 1.0)),
                            new Signal<>(Instant.ofEpochMilli(timestamp), new ZoneStatus(zoneSettings, new CallingStatus(null, 3.0625, true), null, null), "thermostat-west")),
            new ImmutablePair<>(
                    new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(6.875, 1.0)),
                    null),
            new ImmutablePair<>(
                    new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(10.3125, 1.0)),
                    null),
            new ImmutablePair<>(
                    new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(13.6875, 1.0)),
                    null)
            );

            execute(step1, unitQueue, zoneQueue);
            Thread.sleep(SETTLEMENT_DELAY_MILLIS);

            // After that, the demand rises by small increments until the whole thing blows up
            // The count in the crash log is 9, let's make sure it's exceeded

            var demand = 13.6875;
            var batch = new ArrayList<Pair<Signal<UnitControlSignal, Void>, Signal<ZoneStatus, String>>>();

            for (int count = 0; count < 50; count++) {

                timestamp += 50 + rg.nextInt(100);
                demand += rg.nextDouble()/10;

                batch.add(new ImmutablePair<>(
                        new Signal<>(Instant.ofEpochMilli(timestamp), new UnitControlSignal(demand, 1.0)),
                        null));
            }

            execute(Flux.fromIterable(batch), unitQueue, zoneQueue);

            // Terminating everything (what used to be powerOff())

            Flux<Pair<Signal<UnitControlSignal, Void>, Signal<ZoneStatus, String>>> end = Flux.just(
                    new ImmutablePair<>(
                            new Signal<>(Instant.ofEpochMilli(timestamp), null, null, Signal.Status.FAILURE_TOTAL, new IOException("end of flux")),
                            new Signal<>(Instant.ofEpochMilli(timestamp), null, null, Signal.Status.FAILURE_TOTAL, new IOException("end of flux")))
            );

            execute(end, unitQueue, zoneQueue);
            Thread.sleep(SETTLEMENT_DELAY_MILLIS);

            assertThat(switchLivingRoom.getState().block()).isTrue();
            assertThat(switchKitchen.getState().block()).isTrue();
            assertThat(switchWestBathroom.getState().block()).isTrue();
            assertThat(switchWestDamper.getState().block()).isTrue();
            assertThat(switchWestBoosterFan.getState().block()).isTrue();

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void execute(
            Flux<Pair<Signal<UnitControlSignal, Void>, Signal<ZoneStatus, String>>> source,
            LinkedBlockingQueue<Signal<UnitControlSignal, Void>> unitQueue,
            LinkedBlockingQueue<Signal<ZoneStatus, String>> zoneQueue) {

        source
                .doOnNext(p -> {
                    Optional.ofNullable(p.getLeft()).ifPresent(unitSignal -> {
                        try {
                            unitQueue.put(unitSignal);
                        } catch (InterruptedException ex) {
                            throw new IllegalStateException("Oops", ex);
                        }
                    });
                    Optional.ofNullable(p.getRight()).ifPresent(zoneSignal -> {
                        try {
                            zoneQueue.put(zoneSignal);
                        } catch (InterruptedException ex) {
                            throw new IllegalStateException("Oops", ex);
                        }
                    });
                })
                .log()
                .blockLast();
        logger.info("--------------------");
    }
}
