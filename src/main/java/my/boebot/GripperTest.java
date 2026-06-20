package my.boebot;

import com.pi4j.context.Context;

/**
 * GripperTest - Menu Option 7: Test MG90S gripper servo on Servo HAT channel 0.
 *
 * The MG90S is a POSITION servo (not continuous rotation).
 * It moves to a specific angle based on pulse width.
 *
 * Pulse values:
 *   1100 us = one end position (approximately -60 degrees from center)
 *   1500 us = center position (neutral)
 *   1900 us = other end position (approximately +60 degrees from center)
 *
 * WARNING: Do NOT force the gripper past its mechanical end stops.
 *          This will damage the servo gears.
 *
 * Sequence: 1100us -> 1500us -> 1900us -> 1500us
 * With short delays between each movement.
 */
public class GripperTest {

    // Servo HAT channel for the MG90S gripper
    private static final int GRIPPER_CH = 0;

    // Pulse values (microseconds) for MG90S position servo
    private static final int PULSE_MIN    = 1100;  // One end of travel
    private static final int PULSE_CENTER = 1500;  // Center / neutral
    private static final int PULSE_MAX    = 1900;  // Other end of travel

    // Delay between movements (ms)
    private static final int MOVE_DELAY_MS = 700;

    public static boolean run(AppLogger logger, BotConfig config, Context pi4j) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 7: MG90S Gripper CH0");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + GRIPPER_CH);
        System.out.println("  Servo type        : MG90S position servo");
        System.out.println();
        System.out.println("  WARNING: Do NOT force the gripper past its");
        System.out.println("           mechanical end stops.");
        System.out.println("           This will damage the servo gears.");
        System.out.println();
        System.out.println("  Make sure the gripper is free to move.");

        logger.logSeparator();
        logger.log("TEST 7: MG90S Gripper Servo - Servo HAT channel " + GRIPPER_CH);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Gripper Test", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            // Step 1: Move to center first (safe starting position)
            System.out.println("[Step 1] Center position - 1500 us ...");
            logger.log("  CH0 -> 1500 us (center)");
            pca.setServoPulse(GRIPPER_CH, PULSE_CENTER);
            Thread.sleep(MOVE_DELAY_MS);

            // Step 2: Move to minimum (1100 us)
            System.out.println("[Step 2] Moving to 1100 us (min) ...");
            logger.log("  CH0 -> 1100 us (min)");
            pca.setServoPulse(GRIPPER_CH, PULSE_MIN);
            Thread.sleep(MOVE_DELAY_MS);

            // Step 3: Move to center (1500 us)
            System.out.println("[Step 3] Moving to 1500 us (center) ...");
            logger.log("  CH0 -> 1500 us (center)");
            pca.setServoPulse(GRIPPER_CH, PULSE_CENTER);
            Thread.sleep(MOVE_DELAY_MS);

            // Step 4: Move to maximum (1900 us)
            System.out.println("[Step 4] Moving to 1900 us (max) ...");
            logger.log("  CH0 -> 1900 us (max)");
            pca.setServoPulse(GRIPPER_CH, PULSE_MAX);
            Thread.sleep(MOVE_DELAY_MS);

            // Step 5: Return to center (safe resting position)
            System.out.println("[Step 5] Returning to 1500 us (center) ...");
            logger.log("  CH0 -> 1500 us (center - final)");
            pca.setServoPulse(GRIPPER_CH, PULSE_CENTER);
            Thread.sleep(MOVE_DELAY_MS);

            logger.logPass("MG90S Gripper Servo CH0");
            System.out.println();
            System.out.println("[RESULT] PASS - Gripper servo test complete.");
            System.out.println("  Gripper returned to center position (1500 us).");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("MG90S Gripper CH0", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                // Return to center before closing
                try { pca.setServoPulse(GRIPPER_CH, PULSE_CENTER); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }
}
