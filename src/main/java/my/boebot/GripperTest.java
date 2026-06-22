package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * GripperTest - Menu Option 9: interactive MG90S gripper servo on channel 0.
 *
 * Incremental control so the gripper never slams into its mechanical end stops
 * (which makes the MG90S stall, draw heavy current and can hang the board).
 * Each press nudges the position by a small step within a safe range:
 *
 *   8   = close a step   (toward CLOSE_LIMIT)
 *   2   = open a step     (toward OPEN_LIMIT)
 *   5   = stop (hold current position)
 *   ESC / q = exit (gripper stays where it is)
 *
 * The position is clamped to OPEN_LIMIT..CLOSE_LIMIT, which are kept away from
 * the 1100/1900 extremes so the servo is never forced past its travel. Widen or
 * narrow these limits to match your gripper.
 */
public class GripperTest {

    private static final int GRIPPER_CH  = 0;

    private static final int OPEN_LIMIT  = 1250;  // most-open (safe, not max)
    private static final int CLOSE_LIMIT = 1750;  // most-closed (safe, not max)
    private static final int CENTER      = 1500;
    private static final int STEP        = 40;    // us per key press

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 9: MG90S Gripper CH0 (interactive)");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + GRIPPER_CH);
        System.out.println("  Servo type        : MG90S position servo");
        System.out.println();
        System.out.println("  Controls:  8 = close step   2 = open step");
        System.out.println("             5 = stop         ESC / q = exit");
        System.out.println("  Range is limited to " + OPEN_LIMIT + ".." + CLOSE_LIMIT
            + " us so the gripper never jams.");

        logger.logSeparator();
        logger.log("TEST 9: MG90S Gripper (interactive) - Servo HAT channel " + GRIPPER_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Gripper Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            int pos = CENTER;
            pca.setServoPulse(GRIPPER_CH, pos);

            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("  Centered (" + CENTER + " us). Press 8 / 2 / 5, or ESC (or q) to exit...");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        pos = Math.min(pos + STEP, CLOSE_LIMIT);
                        pca.setServoPulse(GRIPPER_CH, pos);
                        System.out.println("  [8] CLOSE step -> " + pos + " us"
                            + (pos == CLOSE_LIMIT ? "  (close limit)" : ""));
                        logger.log("  CH0 -> " + pos + " us (close step)");
                    }
                    case '2' -> {
                        pos = Math.max(pos - STEP, OPEN_LIMIT);
                        pca.setServoPulse(GRIPPER_CH, pos);
                        System.out.println("  [2] OPEN step -> " + pos + " us"
                            + (pos == OPEN_LIMIT ? "  (open limit)" : ""));
                        logger.log("  CH0 -> " + pos + " us (open step)");
                    }
                    case '5' -> {
                        pca.setServoPulse(GRIPPER_CH, pos);
                        System.out.println("  [5] STOP (hold at " + pos + " us)");
                        logger.log("  CH0 hold at " + pos + " us (stop)");
                    }
                    case 27, 'q', 'Q', -1 -> {
                        System.out.println("  [exit] Ending gripper test. Held at " + pos + " us.");
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
            ServoSafety.removeStopHook(stopHook);
        }
    }
}
