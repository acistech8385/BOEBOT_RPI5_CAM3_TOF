package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * LeftWheelTest - Menu Option 5: Test left wheel servo on Servo HAT channel 15.
 *
 * The left wheel uses a Parallax continuous rotation servo.
 * Pulse widths:
 *   1500 us = neutral / stop
 *   1450 us = slow movement in one direction
 *   1550 us = slow movement in opposite direction
 *
 * Safety: User must type WHEELS_LIFTED before any movement.
 * Movement duration: max 1 second per direction.
 */
public class LeftWheelTest {

    // Servo pulse values (microseconds)
    private static final int PULSE_STOP  = 1500;  // Neutral - servo stops
    private static final int PULSE_DIR_A = 1450;  // Slow movement direction A
    private static final int PULSE_DIR_B = 1550;  // Slow movement direction B

    // Channel for left wheel
    private static final int LEFT_WHEEL_CH = 15;

    // Movement duration: 1 second max
    private static final int MOVE_MS = 1000;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 5: Left Wheel Servo CH15");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + LEFT_WHEEL_CH);
        System.out.println("  Servo type        : Parallax continuous rotation");

        logger.logSeparator();
        logger.log("TEST 5: Left Wheel Servo - Servo HAT channel " + LEFT_WHEEL_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Left Wheel Test", "Pi4J not available");
            return false;
        }

        // SAFETY CHECK - must type WHEELS_LIFTED
        if (!WheelSafety.confirmWheelsLifted(scanner)) {
            logger.log("LEFT WHEEL TEST: Skipped - WHEELS_LIFTED not confirmed.");
            return false;
        }

        PCA9685 pca = null;
        try {
            // Initialize PCA9685
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            // Step 1: Set neutral (stop)
            System.out.println("[Step 1] Setting neutral (stop) - 1500 us ...");
            logger.log("  CH15 -> 1500 us (neutral/stop)");
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(500);

            // Step 2: Move direction A (1450 us) for max 1 second
            System.out.println("[Step 2] Moving direction A - 1450 us for 1 second ...");
            logger.log("  CH15 -> 1450 us (direction A) for 1 second");
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_DIR_A);
            Thread.sleep(MOVE_MS);

            // Step 3: Stop
            System.out.println("[Step 3] Stopping - 1500 us ...");
            logger.log("  CH15 -> 1500 us (stop)");
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(500);

            // Step 4: Move direction B (1550 us) for max 1 second
            System.out.println("[Step 4] Moving direction B - 1550 us for 1 second ...");
            logger.log("  CH15 -> 1550 us (direction B) for 1 second");
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_DIR_B);
            Thread.sleep(MOVE_MS);

            // Step 5: Stop
            System.out.println("[Step 5] Stopping - 1500 us ...");
            logger.log("  CH15 -> 1500 us (stop)");
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);
            Thread.sleep(200);

            logger.logPass("Left Wheel Servo CH15");
            System.out.println();
            System.out.println("[RESULT] PASS - Left wheel servo test complete.");
            System.out.println("  Note: If the wheel direction seems reversed, swap 1450/1550 us.");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Left Wheel Servo CH15", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }
}
