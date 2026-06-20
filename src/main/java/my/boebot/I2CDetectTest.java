package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * I2CDetectTest - Menu Option 2: Check I2C bus and detect PCA9685.
 *
 * Steps:
 *   1. Check if /dev/i2c-1 exists
 *   2. Run i2cdetect -y 1
 *   3. Look for address 0x40 in the output (PCA9685 Servo HAT)
 *   4. Print PASS if 0x40 is found, FAIL if not
 */
public class I2CDetectTest {

    // Expected PCA9685 address
    private static final int PCA9685_ADDRESS = 0x40;

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 2: Check I2C Servo HAT");
        System.out.println("====================================");

        logger.logSeparator();
        logger.log("TEST 2: Check I2C Servo HAT");

        boolean pass = true;

        // Step 1: Check if /dev/i2c-1 exists
        int bus = config.getI2cBus();
        String i2cDevice = "/dev/i2c-" + bus;
        File i2cFile = new File(i2cDevice);

        System.out.println("[Step 1] Checking for " + i2cDevice + " ...");
        logger.log("  Checking: " + i2cDevice);

        if (!i2cFile.exists()) {
            System.out.println("  NOT FOUND: " + i2cDevice);
            System.out.println();
            System.out.println("  POSSIBLE FIXES:");
            System.out.println("  - Enable I2C:  sudo raspi-config  -> Interface Options -> I2C -> Enable");
            System.out.println("  - Or run:      sudo raspi-config nonint do_i2c 0");
            System.out.println("  - Then reboot: sudo reboot");
            logger.logFail("I2C Device Check", i2cDevice + " not found");
            return false;
        } else {
            System.out.println("  FOUND: " + i2cDevice + "  OK");
            logger.log("  FOUND: " + i2cDevice);
        }

        // Step 2: Run i2cdetect -y 1
        System.out.println();
        System.out.println("[Step 2] Running: sudo i2cdetect -y " + bus);
        logger.log("  Running: i2cdetect -y " + bus);

        try {
            // i2cdetect may need sudo on some systems
            // Try without sudo first, fall back to sudo
            String output = runI2cDetect(bus);

            if (output == null || output.isBlank()) {
                System.out.println("  ERROR: i2cdetect produced no output.");
                System.out.println("  Install i2c-tools:  sudo apt-get install -y i2c-tools");
                logger.logFail("I2C Detect", "i2cdetect produced no output");
                return false;
            }

            // Print the i2cdetect grid
            System.out.println();
            System.out.println(output);
            logger.log("i2cdetect output:");
            for (String line : output.split("\n")) {
                logger.log("  " + line);
            }

            // Step 3: Check if 0x40 appears in the output
            // In i2cdetect output, 0x40 appears as "40" in the grid
            String addressHex = String.format("%02x", PCA9685_ADDRESS); // "40"
            boolean found = output.toLowerCase().contains(" " + addressHex);

            System.out.println("[Step 3] Looking for address 0x"
                + addressHex.toUpperCase() + " in i2cdetect output...");
            logger.log("  Looking for address 0x" + addressHex.toUpperCase());

            if (found) {
                System.out.println("  FOUND: PCA9685 at address 0x"
                    + addressHex.toUpperCase() + "  OK");
                logger.log("  FOUND: PCA9685 at 0x" + addressHex.toUpperCase());
                logger.logPass("I2C Servo HAT Check");
                System.out.println();
                System.out.println("[RESULT] PASS - PCA9685 Servo HAT detected at 0x40.");
                pass = true;
            } else {
                System.out.println("  NOT FOUND: PCA9685 address 0x"
                    + addressHex.toUpperCase() + " not detected.");
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Check that the PCA9685 Servo HAT is properly seated on the GPIO pins.");
                System.out.println("  - Check power: the HAT may need 5V through its power connector.");
                System.out.println("  - Check I2C address jumpers on the HAT (A0-A5 solder pads).");
                System.out.println("    Default = all open = address 0x40.");
                logger.logFail("I2C Servo HAT Check", "0x40 not found in i2cdetect output");
                System.out.println();
                System.out.println("[RESULT] FAIL - PCA9685 not detected.");
                pass = false;
            }

        } catch (Exception e) {
            System.out.println("  ERROR running i2cdetect: " + e.getMessage());
            System.out.println("  Install i2c-tools: sudo apt-get install -y i2c-tools");
            logger.logFail("I2C Detect", e.getMessage());
            pass = false;
        }

        return pass;
    }

    private static String runI2cDetect(int bus) throws Exception {
        StringBuilder output = new StringBuilder();

        // Try with sudo first (needed on some RPi configs)
        ProcessBuilder pb = new ProcessBuilder("sudo", "i2cdetect", "-y", String.valueOf(bus));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();

        // If sudo failed, try without sudo
        if (exitCode != 0 || output.toString().contains("command not found")) {
            output.setLength(0);
            ProcessBuilder pb2 = new ProcessBuilder("i2cdetect", "-y", String.valueOf(bus));
            pb2.redirectErrorStream(true);
            Process process2 = pb2.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process2.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process2.waitFor();
        }

        return output.toString();
    }
}
