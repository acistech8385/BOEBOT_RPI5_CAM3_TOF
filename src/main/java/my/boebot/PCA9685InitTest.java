package my.boebot;

import com.pi4j.context.Context;

/**
 * PCA9685InitTest - Menu Option 3: Test PCA9685 Servo HAT initialization.
 *
 * Opens the PCA9685 over I2C and sets the PWM frequency to 50 Hz.
 * Does NOT move any servo during this test.
 *
 * PASS = PCA9685 responds to I2C and initializes successfully.
 * FAIL = I2C error (check wiring, I2C enabled, HAT address).
 */
public class PCA9685InitTest {

    public static boolean run(AppLogger logger, BotConfig config, Context pi4j) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 3: Test PCA9685 Servo HAT");
        System.out.println("====================================");
        System.out.println("  I2C Bus     : " + config.getI2cBus());
        System.out.println("  I2C Address : 0x"
            + Integer.toHexString(config.getI2cAddress()).toUpperCase());
        System.out.println("  PWM Freq    : " + config.getPwmFrequency() + " Hz");
        System.out.println();
        System.out.println("  NOTE: No servos will move during this test.");

        logger.logSeparator();
        logger.log("TEST 3: PCA9685 Servo HAT Initialization");
        logger.log("  I2C Bus     : " + config.getI2cBus());
        logger.log("  I2C Address : 0x" + Integer.toHexString(config.getI2cAddress()).toUpperCase());
        logger.log("  PWM Freq    : " + config.getPwmFrequency() + " Hz");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            System.out.println("  Pi4J requires a Raspberry Pi with I2C enabled.");
            logger.logFail("PCA9685 Init", "Pi4J context not available");
            return false;
        }

        PCA9685 pca = null;
        try {
            System.out.println("[Step 1] Opening I2C connection to PCA9685...");
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());

            System.out.println("[Step 2] Initializing PCA9685 at "
                + config.getPwmFrequency() + " Hz...");
            pca.initialize(config.getPwmFrequency());

            System.out.println("[Step 3] PCA9685 initialized successfully.");
            System.out.println("  PWM frequency: " + config.getPwmFrequency() + " Hz");
            System.out.println("  No servos were moved.");

            logger.logPass("PCA9685 Servo HAT Initialization");
            System.out.println();
            System.out.println("[RESULT] PASS - PCA9685 Servo HAT initialized at 50 Hz.");
            return true;

        } catch (Exception e) {
            System.out.println("[RESULT] FAIL - Could not initialize PCA9685.");
            System.out.println("  Error: " + e.getMessage());
            System.out.println();
            System.out.println("  POSSIBLE FIXES:");
            System.out.println("  - Check I2C is enabled: sudo i2cdetect -y 1");
            System.out.println("  - Check the Servo HAT is properly seated on the GPIO pins.");
            System.out.println("  - Check that /dev/i2c-1 exists.");
            System.out.println("  - Try: sudo usermod -aG i2c $USER  then log out and back in.");
            logger.logFail("PCA9685 Init", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                pca.close();
            }
        }
    }
}
