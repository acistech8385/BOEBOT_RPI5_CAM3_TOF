package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * GripperTest - Menu Option 7: interactive MG90S gripper servo on Servo HAT channel 0.
 *
 * The MG90S is a POSITION servo. Key controls (no Enter needed):
 *   8   = close gripper
 *   2   = open gripper
 *   5   = stop (hold current position)
 *   ESC = exit the test (gripper stays where it is)
 *
 * Pulse values: 1100..1900 us. CLOSE = 1900, OPEN = 1100, CENTER = 1500.
 * If 8/2 feel reversed, swap PULSE_CLOSE and PULSE_OPEN.
 *
 * WARNING: Do NOT force the gripper past its mechanical end stops -
 *          this damages the servo gears.
 */
public class GripperTest {

    private static final int GRIPPER_CH  = 0;

    private static final int PULSE_OPEN   = 1100;  // Gripper open
    private static final int PULSE_CENTER = 1500;  // Center / neutral
    private static final int PULSE_CLOSE  = 1900;  // Gripper closed

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 7: MG90S Gripper CH0 (interactive)");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + GRIPPER_CH);
        System.out.println("  Servo type        : MG90S position servo");
        System.out.println();
        System.out.println("  Controls:  8 = close   2 = open   5 = stop   ESC = exit");
        System.out.println("  WARNING: do NOT force the gripper past its end stops.");

        logger.logSeparator();
        logger.log("TEST 7: MG90S Gripper (interactive) - Servo HAT channel " + GRIPPER_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Gripper Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            int currentPulse = PULSE_CENTER;
            pca.setServoPulse(GRIPPER_CH, currentPulse);

            System.out.println();
            System.out.println("  Centered (1500 us). Press 8 / 2 / 5, or ESC to exit...");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        currentPulse = PULSE_CLOSE;
                        pca.setServoPulse(GRIPPER_CH, currentPulse);
                        System.out.println("  [8] CLOSE (CH0 -> 1900 us)");
                        logger.log("  CH0 -> 1900 us (close)");
                    }
                    case '2' -> {
                        currentPulse = PULSE_OPEN;
                        pca.setServoPulse(GRIPPER_CH, currentPulse);
                        System.out.println("  [2] OPEN  (CH0 -> 1100 us)");
                        logger.log("  CH0 -> 1100 us (open)");
                    }
                    case '5' -> {
                        pca.setServoPulse(GRIPPER_CH, currentPulse);
                        System.out.println("  [5] STOP  (hold at " + currentPulse + " us)");
                        logger.log("  CH0 hold at " + currentPulse + " us (stop)");
                    }
                    case 27, -1 -> {
                        System.out.println("  [ESC] Exiting gripper test. Held at " + currentPulse + " us.");
                        exit = true;
                    }
                    default -> { /* ignore other keys */ }
                }
            }
            RawKey.restoreMode();

            logger.logPass("MG90S Gripper Servo CH0 (interactive)");
            System.out.println();
            System.out.println("[RESULT] PASS - Gripper interactive test ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("MG90S Gripper CH0", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                pca.close();
            }
        }
    }
}
