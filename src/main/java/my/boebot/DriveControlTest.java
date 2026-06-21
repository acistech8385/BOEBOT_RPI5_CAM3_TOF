package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * DriveControlTest - Menu Option 7: drive the robot with both wheels.
 *
 * Continuous control (no Enter needed):
 *   8   = forward       (both wheels forward)
 *   2   = backward      (both wheels backward)
 *   6   = turn right    (right wheel back, left wheel forward)
 *   4   = turn left     (right wheel forward, left wheel back)
 *   5   = stop
 *   ESC / q = stop and exit
 *
 * The robot keeps moving in the chosen direction until you press 5 (or another
 * direction). Requires the wheels trimmed to a true stop at 1500 us (option 4).
 * Ctrl+C is safe (all servo outputs are cut).
 */
public class DriveControlTest {

    static final int RIGHT_CH = 14;
    static final int LEFT_CH  = 15;

    static final int STOP      = 1500;
    static final int R_FORWARD = 1450;
    static final int R_BACK    = 1550;
    static final int L_FORWARD = 1550;
    static final int L_BACK    = 1450;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 7: Drive Control");
        System.out.println("====================================");
        System.out.println("  Right wheel : Servo HAT channel " + RIGHT_CH);
        System.out.println("  Left wheel  : Servo HAT channel " + LEFT_CH);
        System.out.println();
        System.out.println("  Controls:  8 = forward   2 = backward");
        System.out.println("             4 = turn left 6 = turn right");
        System.out.println("             5 = stop      ESC / q = exit");
        System.out.println("  Reminder: lift the wheels off the ground first.");

        logger.logSeparator();
        logger.log("TEST 7: Drive Control - CH14 + CH15");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Drive Control", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());
            stopBoth(pca);

            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("  Ready. Drive with 8 / 2 / 4 / 6, 5 = stop, ESC/q = exit.");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            RawKey.enableRawMode();
            driveLoop(pca, logger);
            RawKey.restoreMode();

            logger.logPass("Drive Control CH14+CH15");
            System.out.println();
            System.out.println("[RESULT] PASS - Drive control ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Drive Control", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { stopBoth(pca); } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
        }
    }

    /**
     * Reusable drive loop: reads keys and drives until the user exits.
     * Assumes raw key mode is already enabled by the caller.
     */
    static void driveLoop(PCA9685 pca, AppLogger logger) throws Exception {
        boolean exit = false;
        while (!exit) {
            int key = RawKey.readKey();
            switch (key) {
                case '8' -> {
                    set(pca, R_FORWARD, L_FORWARD);
                    System.out.println("  [8] FORWARD");
                    logger.log("  DRIVE forward");
                }
                case '2' -> {
                    set(pca, R_BACK, L_BACK);
                    System.out.println("  [2] BACKWARD");
                    logger.log("  DRIVE backward");
                }
                case '6' -> {
                    set(pca, R_BACK, L_FORWARD);
                    System.out.println("  [6] TURN RIGHT");
                    logger.log("  DRIVE turn right");
                }
                case '4' -> {
                    set(pca, R_FORWARD, L_BACK);
                    System.out.println("  [4] TURN LEFT");
                    logger.log("  DRIVE turn left");
                }
                case '5' -> {
                    stopBoth(pca);
                    System.out.println("  [5] STOP");
                    logger.log("  DRIVE stop");
                }
                case 27, 'q', 'Q', -1 -> {
                    stopBoth(pca);
                    System.out.println("  [exit] Stopping drive control.");
                    exit = true;
                }
                default -> { /* ignore other keys */ }
            }
        }
    }

    static void set(PCA9685 pca, int rightPulse, int leftPulse) throws Exception {
        pca.setServoPulse(RIGHT_CH, rightPulse);
        pca.setServoPulse(LEFT_CH, leftPulse);
    }

    static void stopBoth(PCA9685 pca) throws Exception {
        pca.setServoPulse(RIGHT_CH, STOP);
        pca.setServoPulse(LEFT_CH, STOP);
    }
}
