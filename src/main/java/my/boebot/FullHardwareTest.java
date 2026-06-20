package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * FullHardwareTest - Menu Option 10: Run all hardware tests in sequence.
 *
 * Runs these tests in order:
 *   1. System info
 *   2. I2C detect
 *   3. PCA9685 init
 *   4. Gripper servo (CH0)
 *   5. Camera Module 3 (CAM0)
 *   6. ArduCam ToF (CAM1)
 *   7. Wheel servos (optional - skipped unless WHEELS_LIFTED confirmed)
 *
 * At the end, prints a summary of all test results.
 *
 * Safety: Wheel tests are SKIPPED unless the user confirms WHEELS_LIFTED.
 * The full test asks once at the start whether wheels are lifted.
 */
public class FullHardwareTest {

    public static void run(AppLogger logger, BotConfig config,
                            Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("============================================");
        System.out.println("  Test 10: Full Safe Hardware Test");
        System.out.println("============================================");
        System.out.println("  Running all BOEBOT hardware tests.");
        System.out.println("  Wheel tests will be asked separately.");
        System.out.println();

        logger.logSeparator();
        logger.log("TEST 10: Full Safe Hardware Test - Begin");

        // Track results
        boolean sysOk      = false;
        boolean i2cOk      = false;
        boolean pcaOk      = false;
        boolean gripperOk  = false;
        boolean cam3Ok     = false;
        boolean tofOk      = false;
        boolean wheelSkip  = true;   // wheels skipped unless confirmed
        boolean rightOk    = false;
        boolean leftOk     = false;

        // ---- Test 1: System Info ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 1: System Info...");
        sysOk = SystemInfoTest.run(logger);
        pause();

        // ---- Test 2: I2C Detect ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 2: I2C Detect...");
        i2cOk = I2CDetectTest.run(logger, config);
        pause();

        // ---- Test 3: PCA9685 Init ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 3: PCA9685 Init...");
        pcaOk = PCA9685InitTest.run(logger, config, pi4j);
        pause();

        // ---- Test 4: Gripper (CH0) ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 7: Gripper Servo CH0...");
        gripperOk = GripperTest.run(logger, config, pi4j);
        pause();

        // ---- Test 5: Camera Module 3 ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 8: Camera Module 3...");
        cam3Ok = CameraModule3Test.run(logger, config);
        pause();

        // ---- Test 6: ArduCam ToF ----
        System.out.println();
        System.out.println("[FULL TEST] Running Test 9: ArduCam ToF...");
        tofOk = ToFCameraTest.run(logger, config);
        pause();

        // ---- Wheel Tests (optional) ----
        System.out.println();
        System.out.println("[FULL TEST] Wheel servo tests (optional).");
        System.out.println("  The robot wheels will MOVE if you confirm.");
        System.out.println("  Skip this safely by not typing WHEELS_LIFTED.");
        System.out.println();

        if (WheelSafety.confirmWheelsLifted(scanner)) {
            wheelSkip = false;

            // Right wheel
            System.out.println();
            System.out.println("[FULL TEST] Running Test 4: Right Wheel CH14...");
            rightOk = runRightWheelNoSafetyPrompt(logger, config, pi4j);
            pause();

            // Left wheel
            System.out.println();
            System.out.println("[FULL TEST] Running Test 5: Left Wheel CH15...");
            leftOk = runLeftWheelNoSafetyPrompt(logger, config, pi4j);
            pause();

        } else {
            System.out.println("[FULL TEST] Wheel tests SKIPPED (WHEELS_LIFTED not confirmed).");
            logger.log("FULL TEST: Wheel tests skipped by user.");
        }

        // ---- Summary ----
        System.out.println();
        System.out.println("============================================");
        System.out.println("  FULL TEST SUMMARY");
        System.out.println("============================================");
        printResult("Test 1  System Info         ", sysOk);
        printResult("Test 2  I2C Detect          ", i2cOk);
        printResult("Test 3  PCA9685 Init        ", pcaOk);
        if (wheelSkip) {
            System.out.println("  Test 4  Right Wheel CH14    : SKIPPED");
            System.out.println("  Test 5  Left Wheel CH15     : SKIPPED");
        } else {
            printResult("Test 4  Right Wheel CH14    ", rightOk);
            printResult("Test 5  Left Wheel CH15     ", leftOk);
        }
        printResult("Test 7  Gripper CH0         ", gripperOk);
        printResult("Test 8  Camera Module 3 CAM0", cam3Ok);
        printResult("Test 9  ArduCam ToF CAM1    ", tofOk);
        System.out.println("============================================");

        logger.logSeparator();
        logger.log("FULL TEST SUMMARY:");
        logger.log("  System Info   : " + (sysOk ? "PASS" : "FAIL"));
        logger.log("  I2C Detect    : " + (i2cOk ? "PASS" : "FAIL"));
        logger.log("  PCA9685 Init  : " + (pcaOk ? "PASS" : "FAIL"));
        logger.log("  Gripper CH0   : " + (gripperOk ? "PASS" : "FAIL"));
        logger.log("  Camera Module3: " + (cam3Ok ? "PASS" : "FAIL"));
        logger.log("  ToF CAM1      : " + (tofOk ? "PASS" : "FAIL"));
        if (wheelSkip) {
            logger.log("  Right Wheel   : SKIPPED");
            logger.log("  Left Wheel    : SKIPPED");
        } else {
            logger.log("  Right Wheel   : " + (rightOk ? "PASS" : "FAIL"));
            logger.log("  Left Wheel    : " + (leftOk ? "PASS" : "FAIL"));
        }
        logger.log("Full test complete. Log: " + logger.getLogFilePath());
    }

    private static void printResult(String label, boolean pass) {
        String status = pass ? "PASS" : "FAIL";
        System.out.println("  " + label + " : " + status);
    }

    // Runs right wheel test without prompting for WHEELS_LIFTED again
    // (already confirmed once for the full test)
    private static boolean runRightWheelNoSafetyPrompt(AppLogger logger,
            BotConfig config, Context pi4j) {
        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            int ch = config.getRightWheelChannel();
            pca.setServoPulse(ch, 1500); Thread.sleep(300);
            pca.setServoPulse(ch, 1450); Thread.sleep(1000);
            pca.setServoPulse(ch, 1500); Thread.sleep(300);
            pca.setServoPulse(ch, 1550); Thread.sleep(1000);
            pca.setServoPulse(ch, 1500); Thread.sleep(200);

            logger.logPass("Right Wheel CH" + ch + " (Full Test)");
            System.out.println("[RESULT] PASS - Right wheel complete.");
            return true;

        } catch (Exception e) {
            logger.logFail("Right Wheel (Full Test)", e.getMessage());
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            return false;
        } finally {
            if (pca != null) {
                try { pca.setServoPulse(config.getRightWheelChannel(), 1500); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }

    // Runs left wheel test without prompting for WHEELS_LIFTED again
    private static boolean runLeftWheelNoSafetyPrompt(AppLogger logger,
            BotConfig config, Context pi4j) {
        PCA9685 pca = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());

            int ch = config.getLeftWheelChannel();
            pca.setServoPulse(ch, 1500); Thread.sleep(300);
            pca.setServoPulse(ch, 1450); Thread.sleep(1000);
            pca.setServoPulse(ch, 1500); Thread.sleep(300);
            pca.setServoPulse(ch, 1550); Thread.sleep(1000);
            pca.setServoPulse(ch, 1500); Thread.sleep(200);

            logger.logPass("Left Wheel CH" + ch + " (Full Test)");
            System.out.println("[RESULT] PASS - Left wheel complete.");
            return true;

        } catch (Exception e) {
            logger.logFail("Left Wheel (Full Test)", e.getMessage());
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            return false;
        } finally {
            if (pca != null) {
                try { pca.setServoPulse(config.getLeftWheelChannel(), 1500); } catch (Exception ignore) {}
                pca.close();
            }
        }
    }

    // Short pause between tests in the full run
    private static void pause() {
        try { Thread.sleep(300); } catch (InterruptedException ignore) {}
    }
}
