package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * CalibrateNeutralTest - Menu Option 15: calibrate wheel servo neutral (stop).
 *
 * Continuous-rotation servos must be trimmed so they stop completely at the
 * neutral pulse (1500 us). This test holds BOTH wheel channels at 1500 us so
 * you can turn each servo's trim potentiometer until the wheel stops.
 *
 * Both wheels are held at 1500 us. Adjust one servo's pot at a time and watch
 * that wheel - an already-calibrated wheel stays still, so only the wheel that
 * still needs trimming will be moving.
 *
 * Keys (no Enter needed):
 *   5   = re-send 1500 us to both wheels (hold neutral)
 *   ESC / q = exit
 *
 * Lift the wheels off the ground first. Ctrl+C is safe (outputs are cut).
 */
public class CalibrateNeutralTest {

    private static final int RIGHT_CH = 14;
    private static final int LEFT_CH  = 15;
    private static final int NEUTRAL  = 1500;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 15: Calibrate Wheel Neutral (1500 us)");
        System.out.println("====================================");
        System.out.println("  Right wheel : Servo HAT channel " + RIGHT_CH);
        System.out.println("  Left wheel  : Servo HAT channel " + LEFT_CH);
        System.out.println();
        System.out.println("  Both wheels are held at 1500 us (neutral / stop).");
        System.out.println("  Turn each servo's trim potentiometer (small hole on the");
        System.out.println("  side of the servo) with a small screwdriver until that");
        System.out.println("  wheel stops turning completely.");
        System.out.println();
        System.out.println("  Keys:  5 = re-send 1500us    ESC / q = exit");
        System.out.println("  Reminder: lift the wheels off the ground first.");

        logger.logSeparator();
        logger.log("TEST 15: Calibrate Wheel Neutral - hold CH14+CH15 at 1500 us");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Calibrate Neutral", "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            pca.setServoPulse(RIGHT_CH, NEUTRAL);
            pca.setServoPulse(LEFT_CH, NEUTRAL);

            // Safety: cut all servo output even if the user presses Ctrl+C.
            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("  Holding 1500 us on both wheels. Adjust the pots now...");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '5' -> {
                        pca.setServoPulse(RIGHT_CH, NEUTRAL);
                        pca.setServoPulse(LEFT_CH, NEUTRAL);
                        System.out.println("  [5] Re-sent 1500 us to CH14 + CH15.");
                    }
                    case 27, 'q', 'Q', -1 -> {
                        System.out.println("  [exit] Ending neutral calibration.");
                        exit = true;
                    }
                    default -> { /* ignore other keys */ }
                }
            }
            RawKey.restoreMode();

            logger.logPass("Calibrate Wheel Neutral CH14+CH15");
            System.out.println();
            System.out.println("[RESULT] PASS - Neutral calibration ended.");
            System.out.println("  If both wheels stop at 1500 us now, calibration is done.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Calibrate Neutral", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try {
                    pca.setServoPulse(RIGHT_CH, NEUTRAL);
                    pca.setServoPulse(LEFT_CH, NEUTRAL);
                } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
        }
    }
}
