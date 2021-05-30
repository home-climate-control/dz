package net.sf.dz3.device.actuator.servomaster;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import com.homeclimatecontrol.jukebox.sem.ACT;
import com.homeclimatecontrol.jukebox.service.Messenger;
import net.sf.dz3.device.actuator.impl.AbstractDamper;
import net.sf.servomaster.device.model.Servo;
import net.sf.servomaster.device.model.transform.LimitTransformer;
import net.sf.servomaster.device.model.transform.LinearTransformer;
import net.sf.servomaster.device.model.transform.Reverser;
import net.sf.servomaster.device.model.transition.CrawlTransitionController;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;

/**
 * Damper controlled by a RC Servo.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ServoDamper extends AbstractDamper {

    /**
     * Servo to control.
     */
    private final Servo servo;

    /**
     * Create an instance with no reversing and no range or limit calibration.
     *
     * @param name Human readable name.
     * @param servo Servo instance to use.
     */
    public ServoDamper(String name, Servo servo) {
        this(name, servo, false, null, null);
    }

    /**
     * Create an instance with range calibration only.
     *
     * @param name Human readable name.
     * @param servo Servo instance to use.
     * @param reverse {@code true} if the servo movement should be reversed.
     * @param rangeCalibration Range calibration object.
     */
    public ServoDamper(String name, Servo servo, boolean reverse, RangeCalibration rangeCalibration) {
        this(name, servo, reverse, rangeCalibration, null);
    }

    /**
     * Create an instance with limit calibration only.
     *
     * @param name Human readable name.
     * @param servo Servo instance to use.
     * @param reverse {@code true} if the servo movement should be reversed.
     * @param limitCalibration Limit calibration object.
     */
    public ServoDamper(String name, Servo servo, boolean reverse, LimitCalibration limitCalibration) {
        this(name, servo, reverse, null, limitCalibration);
    }

    /**
     * Create an instance.
     *
     * Only one of {@code rangeCalibration} and {@code limitCalibration} can be not null at the same time.
     *
     * @param name Human readable name.
     * @param servo Servo instance to use.
     * @param reverse {@code true} if the servo movement should be reversed.
     * @param rangeCalibration Range calibration object.
     * @param limitCalibration Limit calibration object.
     */
    public ServoDamper(
            String name,
            Servo servo,
            boolean reverse,
            RangeCalibration rangeCalibration,
            LimitCalibration limitCalibration) {

        super(name);

        ThreadContext.push("ServoDamper()");

        try {

            if (servo == null ) {
                throw new IllegalArgumentException("servo can't be null");
            }

            if ((rangeCalibration != null) && (limitCalibration != null)) {
                throw new IllegalArgumentException("Range and limit calibration are mutually exclusive - must specify only one");
            }

            logger.info("reverse: {}", reverse);
            logger.info("range: {}", rangeCalibration);
            logger.info("limit: {}", limitCalibration);

            if (rangeCalibration != null) {

                servo.getMeta().setProperty("servo/range/min", Integer.toString(rangeCalibration.min));
                servo.getMeta().setProperty("servo/range/max", Integer.toString(rangeCalibration.max));
            }

            // Until it is actually done in configuration, let's just install a crawl controller
            // But only if it is specifically requested (see dz-runner script)

            if (System.getProperty(getClass().getName() + ".crawl") != null) {

                logger.info("Will be crawling");
                servo.attach(new CrawlTransitionController(), false);
            }

            if (limitCalibration != null) {

                servo = new LimitTransformer(servo, limitCalibration.min, limitCalibration.max);
            }

            if (reverse) {

                servo = new Reverser(servo);
            }

            // VT: NOTE: This may not always be the case, there will be
            // contraptions with angle range other than 0..180. This
            // will have to be configurable. On the other hand, nobody
            // complained in five years, so it should be fine as is.

            servo = new LinearTransformer(servo);

            this.servo = servo;

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public void moveDamper(double throttle) throws IOException {

        ThreadContext.push("moveDamper");

        try {

            if (Double.compare(servo.getPosition(), throttle) != 0) {
                logger.debug("{}: {}", servo.getName(), throttle);
            }

            servo.setPosition(throttle);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public double getPosition() throws IOException {
        return servo.getPosition();
    }

    @Override
    public ACT park() {

        // VT: This implementation is awkward, but there seems to be no way
        // of avoiding it - an abstraction adapter is required between
        // TransitionCompletionToken from Servomaster and EventSemaphore
        // from Jukebox. The former mustn't be visible beyond park()
        // abstraction layer (there may be other damper implementations that
        // don't use Servomaster), whereas the latter is native for DZ.

        logger.info("{}: parking at {}", servo.getName(), getParkPosition());

        return new ParkingAssistant().start();
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Servo based damper",
                Integer.toHexString(hashCode()),
                "Controls single servo");
    }

    /**
     * Commands the {@link ServoDamper#servo} to move to {@link ServoDamper#getParkPosition
     * parked position} and waits until the servo has done so.
     */
    private class ParkingAssistant extends Messenger {

        /**
         * Move the {@link ServoDamper#servo} and wait until it gets there.
         */
        @SuppressWarnings("squid:S1181")
        @Override
        protected final Object execute() throws Throwable {

            ThreadContext.push("execute");

            try {

                var done = servo.setPosition(getParkPosition());

                if (done == null) {
                    // Happens with mocks, better make note of it
                    throw new IllegalStateException("done == null for " + servo.getName());
                }

                done.get();

                logger.info("{}: parked at {}", servo.getName(), getParkPosition());

            } catch (Throwable t) {

                // squid:S1181: No.
                logger.error("{}: failed to park at {}", servo.getName(), getParkPosition(), t);

            } finally {
                ThreadContext.pop();
            }

            return null;
        }
    }
}
