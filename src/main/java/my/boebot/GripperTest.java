package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * GripperTest - Menu Option 9: interactive MG90S gripper servo on channel 0.
 *
 * The MG90S is a position servo. If it is commanded past its mechanical travel
 * it stalls and draws ~0.6-1 A continuously, which can brown out the board
 * (servo freezes, SSH/RDP drops). To avoid that this test:
 *
 *   - keeps the position inside a safe range (OPEN_LIMIT..CLOSE_LIMIT),
 *   - moves in steps, and
 *   - cuts the channel's PWM after each move, so the servo never *holds* a
 *     stall. The gripper relaxes between moves (this is expected).
 *
 * Keys (no Enter needed):
 *   8   = close a step
 *   2   = open a step
 *   5   = relax (cut PWM now)
 *   ESC / q = exit
 *
 * If the gripper still strains at a limit, narrow OPEN_LIMIT / CLOSE_LIMIT.
 * Also make sure the Servo HAT has its own VIN power - a stalling MG90S can
 * pull more current than the Pi's 5V rail alone can supply.
 */
public class GripperTest {

    private static final int GRIPPER_CH  = 0;

    private static final int OPEN_LIMIT  = 1350;  // most-open (safe)
    private static final int CLOSE_LIMIT = 1650;  // most-closed (safe)
    private static final int CENTER      = 1500;
    private static final int STEP        = 40;    // us per key press (small, gentle)
    private static final int TRAVEL_MS   = 180;   // brief pulse to limit stall time

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
        System.out.println("             5 = relax        ESC / q = exit");
        System.out.println("  Range " + OPEN_LIMIT + ".." + CLOSE_LIMIT
            + " us; output is cut after each move so it never stalls.");

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

            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            int pos = CENTER;
            // Start RELAXED (no pulse). Do NOT auto-move - if the horn is
            // misaligned, commanding centre could stall the servo immediately.
            pca.setOff(GRIPPER_CH);

            System.out.println();
            System.out.println("  Servo relaxed. Press 8 / 2 to inch (from " + CENTER + " us),");
            System.out.println("  5 = relax, ESC (or q) to exit...");
            System.out.println("  If you hear continuous buzzing, the servo is STALLING -");
            System.out.println("  press 5 and re-check the gripper horn / mechanical range.");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        pos = Math.min(pos + STEP, CLOSE_LIMIT);
                        nudge(pca, GRIPPER_CH, pos);
                        System.out.println("  [8] CLOSE step -> " + pos + " us"
                            + (pos == CLOSE_LIMIT ? "  (close limit)" : ""));
                        logger.log("  CH0 -> " + pos + " us (close step)");
                    }
                    case '2' -> {
                        pos = Math.max(pos - STEP, OPEN_LIMIT);
                        nudge(pca, GRIPPER_CH, pos);
                        System.out.println("  [2] OPEN step -> " + pos + " us"
                            + (pos == OPEN_LIMIT ? "  (open limit)" : ""));
                        logger.log("  CH0 -> " + pos + " us (open step)");
                    }
                    case '5' -> {
                        pca.setOff(GRIPPER_CH);
                        System.out.println("  [5] RELAX (output off at " + pos + " us)");
                        logger.log("  CH0 relaxed (output off)");
                    }
                    case 27, 'q', 'Q', -1 -> {
                        pca.setOff(GRIPPER_CH);
                        System.out.println("  [exit] Ending gripper test (relaxed).");
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
                try { pca.setOff(GRIPPER_CH); } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
        }
    }

    /** Drive the servo toward a pulse, give it time to travel, then cut output. */
    private static void nudge(PCA9685 pca, int channel, int pulseUs) throws Exception {
        pca.setServoPulse(channel, pulseUs);
        Thread.sleep(TRAVEL_MS);
        pca.setOff(channel);
    }
}
