package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * LeftWheelTest - Menu Option 5: interactive left wheel servo on Servo HAT channel 15.
 *
 * Parallax continuous rotation servo. Key controls (no Enter needed):
 *   8   = drive forward
 *   2   = drive backward
 *   5   = stop
 *   ESC = stop and exit the test
 *
 * Pulse widths: 1500 = stop, 1550 = forward, 1450 = backward.
 * The left wheel faces the opposite way to the right, so its forward pulse
 * is the mirror of the right wheel's.
 */
public class LeftWheelTest {

    private static final int PULSE_STOP     = 1500;  // Neutral - servo stops
    private static final int PULSE_FORWARD  = 1550;  // Drive forward (mirror of right)
    private static final int PULSE_BACKWARD = 1450;  // Drive backward

    private static final int LEFT_WHEEL_CH = 15;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 5: Left Wheel Servo CH15 (interactive)");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + LEFT_WHEEL_CH);
        System.out.println("  Servo type        : Parallax continuous rotation");
        System.out.println();
        System.out.println("  Controls:  8 = forward   2 = backward   5 = stop   ESC = exit");
        System.out.println("  Reminder: lift the wheels off the ground before driving.");

        logger.logSeparator();
        logger.log("TEST 5: Left Wheel Servo (interactive) - Servo HAT channel " + LEFT_WHEEL_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Left Wheel Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());
            pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);

            // Safety: stop the servo even if the user presses Ctrl+C.
            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("  Ready. Press 8 / 2 / 5, or ESC (or q) to exit...");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        pca.setServoPulse(LEFT_WHEEL_CH, PULSE_FORWARD);
                        System.out.println("  [8] FORWARD  (CH15 -> 1550 us)");
                        logger.log("  CH15 -> 1550 us (forward)");
                    }
                    case '2' -> {
                        pca.setServoPulse(LEFT_WHEEL_CH, PULSE_BACKWARD);
                        System.out.println("  [2] BACKWARD (CH15 -> 1450 us)");
                        logger.log("  CH15 -> 1450 us (backward)");
                    }
                    case '5' -> {
                        pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);
                        System.out.println("  [5] STOP     (CH15 -> 1500 us)");
                        logger.log("  CH15 -> 1500 us (stop)");
                    }
                    case 27, 'q', 'Q', -1 -> {
                        pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP);
                        System.out.println("  [exit] Stopping left wheel test.");
                        exit = true;
                    }
                    default -> { /* ignore other keys */ }
                }
            }
            RawKey.restoreMode();

            logger.logPass("Left Wheel Servo CH15 (interactive)");
            System.out.println();
            System.out.println("[RESULT] PASS - Left wheel interactive test ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Left Wheel Servo CH15", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { pca.setServoPulse(LEFT_WHEEL_CH, PULSE_STOP); } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
        }
    }
}
