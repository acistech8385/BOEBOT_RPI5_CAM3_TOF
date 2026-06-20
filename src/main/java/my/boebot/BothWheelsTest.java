package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * BothWheelsTest - Menu Option 6: Test both wheel servos together.
 *
 * Tests the right wheel (Servo HAT channel 14) and
 * left wheel (Servo HAT channel 15) together.
 *
 * Movement sequence:
 *   1. Both wheels neutral (stop)
 *   2. Short forward test (conservative values)
 *   3. Both wheels stop
 *   4. Short reverse test
 *   5. Both wheels stop
 *
 * Safety: User must type WHEELS_LIFTED before any movement.
 *
 * Note on wheel direction:
 *   Because the left and right wheels face opposite directions on the robot,
 *   "forward" for the right wheel may need the OPPOSITE pulse from the left wheel.
 *   If the robot moves in the wrong direction, the pulse values may need adjustment.
 *   This app will print a reminder to check the direction.
 */
public class BothWheelsTest {

    // Servo HAT channels
    private static final int RIGHT_CH = 14;
    private static final int LEFT_CH  = 15;

    // Pulse values (us)
    private static final int STOP    = 1500;  // Neutral - both wheels stop

    // Conservative forward/reverse values for combined test
    // Right wheel: 1450 us = forward direction
    // Left wheel:  1550 us = forward direction (opposite servo facing)
    // If robot goes backwards, swap these.
    private static final int RIGHT_FORWARD = 1450;
    private static final int LEFT_FORWARD  = 1550;

    private static final int RIGHT_REVERSE = 1550;
    private static final int LEFT_REVERSE  = 1450;

    // Movement duration: 1 second max
    private static final int MOVE_MS = 1000;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 6: Both Wheel Servos");
        System.out.println("====================================");
        System.out.println("  Right wheel : Servo HAT channel " + RIGHT_CH);
        System.out.println("  Left wheel  : Servo HAT channel " + LEFT_CH);
        System.out.println();
        System.out.println("  NOTE: Left and right servos face OPPOSITE directions.");
        System.out.println("  Forward = CH14 at 1450 us + CH15 at 1550 us.");
        System.out.println("  If the robot moves backwards, pulse values need adjustment.");

        logger.logSeparator();
        logger.log("TEST 6: Both Wheel Servos");
        logger.log("  Right wheel : Servo HAT channel " + RIGHT_CH);
        logger.log("  Left wheel  : Servo HAT channel " + LEFT_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Both Wheels Test", "Pi4J not available");
            return false;
        }

        // SAFETY CHECK - must type WHEELS_LIFTED
        if (!WheelSafety.confirmWheelsLifted(scanner)) {
            logger.log("BOTH WHEEL TEST: Skipped - WHEELS_LIFTED not confirmed.");
            return false;
        }

        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            // Step 1: Both wheels neutral (stop)
            System.out.println("[Step 1] Both wheels neutral (stop) - 1500 us ...");
            logger.log("  CH14 + CH15 -> 1500 us (neutral/stop)");
            pca.setServoPulse(RIGHT_CH, STOP);
            pca.setServoPulse(LEFT_CH, STOP);
            Thread.sleep(500);

            // Step 2: Short forward test
            System.out.println("[Step 2] Forward test: CH14=1450us, CH15=1550us for 1 second ...");
            logger.log("  CH14 -> 1450 us, CH15 -> 1550 us (forward) for 1 second");
            pca.setServoPulse(RIGHT_CH, RIGHT_FORWARD);
            pca.setServoPulse(LEFT_CH, LEFT_FORWARD);
            Thread.sleep(MOVE_MS);

            // Step 3: Stop
            System.out.println("[Step 3] Stop - 1500 us ...");
            logger.log("  CH14 + CH15 -> 1500 us (stop)");
            pca.setServoPulse(RIGHT_CH, STOP);
            pca.setServoPulse(LEFT_CH, STOP);
            Thread.sleep(500);

            // Step 4: Short reverse test
            System.out.println("[Step 4] Reverse test: CH14=1550us, CH15=1450us for 1 second ...");
            logger.log("  CH14 -> 1550 us, CH15 -> 1450 us (reverse) for 1 second");
            pca.setServoPulse(RIGHT_CH, RIGHT_REVERSE);
            pca.setServoPulse(LEFT_CH, LEFT_REVERSE);
            Thread.sleep(MOVE_MS);

            // Step 5: Stop
            System.out.println("[Step 5] Stop - 1500 us ...");
            logger.log("  CH14 + CH15 -> 1500 us (stop)");
            pca.setServoPulse(RIGHT_CH, STOP);
            pca.setServoPulse(LEFT_CH, STOP);
            Thread.sleep(200);

            logger.logPass("Both Wheel Servos CH14+CH15");
            System.out.println();
            System.out.println("[RESULT] PASS - Both wheel servos test complete.");
            System.out.println();
            System.out.println("  >>> If the wheel direction was wrong:");
            System.out.println("      Swap the forward/reverse pulse values in BothWheelsTest.java");
            System.out.println("      RIGHT_FORWARD = 1550, LEFT_FORWARD = 1450 (or vice versa).");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Both Wheel Servos", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try {
                    pca.setServoPulse(RIGHT_CH, STOP);
                    pca.setServoPulse(LEFT_CH, STOP);
                } catch (Exception ignore) {}
                pca.close();
            }
        }
    }
}
