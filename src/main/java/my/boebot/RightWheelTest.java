package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * RightWheelTest - Menu Option 4: interactive right wheel servo on Servo HAT channel 14.
 *
 * Parallax continuous rotation servo. Key controls (no Enter needed):
 *   8   = drive forward
 *   2   = drive backward
 *   5   = stop
 *   ESC = stop and exit the test
 *
 * Pulse widths: 1500 = stop, 1450 = forward, 1550 = backward.
 * (Right and left wheels face opposite ways; "forward" differs per side.)
 */
public class RightWheelTest {

    private static final int PULSE_STOP     = 1500;  // Neutral - servo stops
    private static final int PULSE_FORWARD  = 1450;  // Drive forward
    private static final int PULSE_BACKWARD = 1550;  // Drive backward

    private static final int RIGHT_WHEEL_CH = 14;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 4: Right Wheel Servo CH14 (interactive)");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + RIGHT_WHEEL_CH);
        System.out.println("  Servo type        : Parallax continuous rotation");
        System.out.println();
        System.out.println("  Controls:  8 = forward   2 = backward   5 = stop   ESC = exit");
        System.out.println("  Reminder: lift the wheels off the ground before driving.");

        logger.logSeparator();
        logger.log("TEST 4: Right Wheel Servo (interactive) - Servo HAT channel " + RIGHT_WHEEL_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Right Wheel Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());
            pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);

            System.out.println();
            System.out.println("  Ready. Press 8 / 2 / 5, or ESC to exit...");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_FORWARD);
                        System.out.println("  [8] FORWARD  (CH14 -> 1450 us)");
                        logger.log("  CH14 -> 1450 us (forward)");
                    }
                    case '2' -> {
                        pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_BACKWARD);
                        System.out.println("  [2] BACKWARD (CH14 -> 1550 us)");
                        logger.log("  CH14 -> 1550 us (backward)");
                    }
                    case '5' -> {
                        pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);
                        System.out.println("  [5] STOP     (CH14 -> 1500 us)");
                        logger.log("  CH14 -> 1500 us (stop)");
                    }
                    case 27, -1 -> {
                        pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP);
                        System.out.println("  [ESC] Exiting right wheel test. Stopped.");
                        exit = true;
                    }
                    default -> { /* ignore other keys */ }
                }
            }
            RawKey.restoreMode();

            logger.logPass("Right Wheel Servo CH14 (interactive)");
            System.out.println();
            System.out.println("[RESULT] PASS - Right wheel interactive test ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Right Wheel Servo CH14", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { pca.setServoPulse(RIGHT_WHEEL_CH, PULSE_STOP); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }
}
