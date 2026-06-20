package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * RightWheelTest - Menu Option 4: Test right wheel servo on Servo HAT channel 14.
 *
 * The right wheel uses a Parallax continuous rotation servo.
 * Pulse widths:
 *   1500 us = neutral / stop
 *   1450 us = slow movement in one direction
 *   1550 us = slow movement in opposite direction
 *
 * Safety: User must type WHEELS_LIFTED before any movement.
 * Movement duration: max 1 second per direction.
 */
public class RightWheelTest {

    // Servo pulse values (microseconds)
    private static final int PULSE_STOP    = 1500;  // Neutral - servo stops
    private static final int PULSE_DIR_A   = 1450;  // Slow movement direction A
    private static final int PULSE_DIR_B   = 1550;  // Slow movement direction B

    // Channel for right wheel
    private static final int RIGHT_WHEEL_CH = 14;

    // Movement duration: 1 second max
    private static final int MOVE_MS = 1000;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 4: Right Wheel Servo CH14");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + RIGHT_WHEEL_CH);
        System.out.println("  Servo type        : Parallax continuous rotation");

        logger.logSeparator();
        logger.log("TEST 4: Right Wheel Servo - Servo HAT channel " + RIGHT_WHEEL_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Right Wheel Test", "Pi4J not available");
            return false;
        }

        // SAFETY CHECK - must type WHEELS_LIFTED
        if (!WheelSafety.confirmWheelsLifted(scanner)) {
            logger.log("RIGHT WHEEL TEST: Skipped - WHEELS_LIFTED not confirmed.");
            return false;
        }

        PCA9685 pca = null;
        try {
            // Initialize PCA9685
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            // Step 1: Set neutral (stop)
            System.out.println("[Step 1] Setting neutral (stop) - 1500 us ...");
            logger.log("  CH14 -> 1500 us (neutral/stop)");
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(500);

            // Step 2: Move direction A (1450 us) for max 1 second
            System.out.println("[Step 2] Moving direction A - 1450 us for 1 second ...");
            logger.log("  CH14 -> 1450 us (direction A) for 1 second");
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_DIR_A);
            Thread.sleep(MOVE_MS);

            // Step 3: Stop
            System.out.println("[Step 3] Stopping - 1500 us ...");
            logger.log("  CH14 -> 1500 us (stop)");
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(500);

            // Step 4: Move direction B (1550 us) for max 1 second
            System.out.println("[Step 4] Moving direction B - 1550 us for 1 second ...");
            logger.log("  CH14 -> 1550 us (direction B) for 1 second");
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_DIR_B);
            Thread.sleep(MOVE_MS);

            // Step 5: Stop
            System.out.println("[Step 5] Stopping - 1500 us ...");
            logger.log("  CH14 -> 1500 us (stop)");
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(200);

            logger.logPass("Right Wheel Servo CH14");
            System.out.println();
            System.out.println("[RESULT] PASS - Right wheel servo test complete.");
            System.out.println("  Note: If the wheel direction seems reversed, swap 1450/1550 us.");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Right Wheel Servo CH14", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                // Make sure servo is at neutral before closing
                try { pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }
}
