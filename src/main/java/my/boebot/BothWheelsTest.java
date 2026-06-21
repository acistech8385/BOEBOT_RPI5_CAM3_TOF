package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * BothWheelsTest - Menu Option 6: automated movement sequence for both wheels.
 *
 * Right wheel = Servo HAT channel 14, Left wheel = Servo HAT channel 15
 * (Parallax continuous rotation servos).
 *
 * Sequence (each move 2 s, 1 s pause between):
 *   1. Forward          2 s
 *   2. Pause            1 s
 *   3. Backward         2 s
 *   4. Pause            1 s + 1 s
 *   5. Turn right       2 s  (right wheel forward, left wheel backward)
 *   6. Pause            1 s
 *   7. Turn left        2 s  (left wheel forward, right wheel backward)
 *   8. Pause            1 s
 *   9. Stop and end
 *
 * Pulse values: 1500 = stop.
 *   Right wheel: 1450 = forward, 1550 = backward.
 *   Left wheel:  1550 = forward, 1450 = backward (faces opposite way).
 */
public class BothWheelsTest {

    private static final int RIGHT_CH = 14;
    private static final int LEFT_CH  = 15;

    private static final int STOP = 1500;

    // Per-wheel forward / backward (left is mirrored vs right)
    private static final int RIGHT_FORWARD  = 1450;
    private static final int RIGHT_BACKWARD = 1550;
    private static final int LEFT_FORWARD   = 1550;
    private static final int LEFT_BACKWARD  = 1450;

    private static final int MOVE_MS  = 2000;  // each motion lasts 2 seconds
    private static final int DELAY_MS = 1000;  // pause between motions

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 6: Both Wheel Servos (auto sequence)");
        System.out.println("====================================");
        System.out.println("  Right wheel : Servo HAT channel " + RIGHT_CH);
        System.out.println("  Left wheel  : Servo HAT channel " + LEFT_CH);
        System.out.println();
        System.out.println("  Sequence: forward 2s -> back 2s -> turn right 2s -> turn left 2s");
        System.out.println("  Reminder: lift the wheels off the ground before this runs.");

        logger.logSeparator();
        logger.log("TEST 6: Both Wheel Servos (auto sequence)");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Both Wheels Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            // Start stopped
            stopBoth(pca);
            Thread.sleep(500);

            // 1. Forward 2 s
            System.out.println("[1] FORWARD 2s (CH14=1450, CH15=1550)...");
            logger.log("  FORWARD: CH14=1450, CH15=1550 for 2s");
            move(pca, RIGHT_FORWARD, LEFT_FORWARD, MOVE_MS);

            // 2. Pause 1 s
            System.out.println("[2] Pause 1s...");
            stopBoth(pca);
            Thread.sleep(DELAY_MS);

            // 3. Backward 2 s
            System.out.println("[3] BACKWARD 2s (CH14=1550, CH15=1450)...");
            logger.log("  BACKWARD: CH14=1550, CH15=1450 for 2s");
            move(pca, RIGHT_BACKWARD, LEFT_BACKWARD, MOVE_MS);

            // 4. Pause 1 s + 1 s
            System.out.println("[4] Pause 1s...");
            stopBoth(pca);
            Thread.sleep(DELAY_MS);
            System.out.println("    Pause 1s...");
            Thread.sleep(DELAY_MS);

            // 5. Turn right 2 s: right wheel forward, left wheel backward
            System.out.println("[5] TURN RIGHT 2s (right fwd, left back)...");
            logger.log("  TURN RIGHT: CH14=" + RIGHT_FORWARD + " (fwd), CH15=" + LEFT_BACKWARD + " (back) for 2s");
            move(pca, RIGHT_FORWARD, LEFT_BACKWARD, MOVE_MS);

            // 6. Pause 1 s
            System.out.println("[6] Pause 1s...");
            stopBoth(pca);
            Thread.sleep(DELAY_MS);

            // 7. Turn left 2 s: left wheel forward, right wheel backward
            System.out.println("[7] TURN LEFT 2s (left fwd, right back)...");
            logger.log("  TURN LEFT: CH15=" + LEFT_FORWARD + " (fwd), CH14=" + RIGHT_BACKWARD + " (back) for 2s");
            move(pca, RIGHT_BACKWARD, LEFT_FORWARD, MOVE_MS);

            // 8. Pause 1 s
            System.out.println("[8] Pause 1s...");
            stopBoth(pca);
            Thread.sleep(DELAY_MS);

            // 9. Stop and end
            System.out.println("[9] STOP. Sequence complete.");
            stopBoth(pca);
            Thread.sleep(200);

            logger.logPass("Both Wheel Servos CH14+CH15 (auto sequence)");
            System.out.println();
            System.out.println("[RESULT] PASS - Movement sequence complete.");
            System.out.println("  If a direction looked wrong, swap that wheel's forward/backward pulses.");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Both Wheel Servos", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { stopBoth(pca); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }

    private static void move(PCA9685 pca, int rightPulse, int leftPulse, int ms)
            throws Exception {
        pca.setServoPulse(RIGHT_CH, rightPulse);
        pca.setServoPulse(LEFT_CH, leftPulse);
        Thread.sleep(ms);
    }

    private static void stopBoth(PCA9685 pca) throws Exception {
        pca.setServoPulse(RIGHT_CH, STOP);
        pca.setServoPulse(LEFT_CH, STOP);
    }
}
