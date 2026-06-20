package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ToFCameraTest - Menu Option 9: Check ArduCam ToF Camera on CAM1.
 *
 * This test does NOT start the ToF camera preview or motor.
 * It is a DETECTION and SETUP CHECK only.
 *
 * Steps:
 *   1. Check if ~/Arducam_tof_camera folder exists.
 *   2. If found, list demo/example files.
 *   3. If NOT found, print instructions to install the ArduCam SDK.
 *
 * The ArduCam ToF camera SDK is installed separately by the user
 * following the official ArduCam instructions.
 */
public class ToFCameraTest {

    // Expected ArduCam SDK folder in the user's home directory
    private static final String ARDUCAM_FOLDER = "Arducam_tof_camera";

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 9: ArduCam ToF CAM1");
        System.out.println("====================================");
        System.out.println("  Camera port  : CAM/DISP 1");
        System.out.println("  SDK folder   : ~/" + ARDUCAM_FOLDER);
        System.out.println();
        System.out.println("  NOTE: This is a DETECTION CHECK only.");
        System.out.println("  The ToF camera preview will NOT be started.");

        logger.logSeparator();
        logger.log("TEST 9: ArduCam ToF Camera - CAM/DISP 1");

        // Build path to ~/Arducam_tof_camera
        String homeDir = System.getProperty("user.home", "/home/pi");
        Path tofPath = Paths.get(homeDir, ARDUCAM_FOLDER);

        System.out.println();
        System.out.println("[Step 1] Checking for: " + tofPath);
        logger.log("  Checking for: " + tofPath);

        if (!Files.exists(tofPath)) {
            // SDK not found - print installation instructions
            System.out.println();
            System.out.println("  NOT FOUND: " + tofPath);
            System.out.println();
            System.out.println("  ArduCam ToF SDK not found.");
            System.out.println("  Install later using the official ArduCam SDK instructions.");
            System.out.println();
            System.out.println("  Official ArduCam ToF Camera GitHub:");
            System.out.println("  https://github.com/ArduCAM/Arducam_tof_camera");
            System.out.println();
            System.out.println("  Quick install steps (on the Raspberry Pi):");
            System.out.println("    cd ~");
            System.out.println("    git clone https://github.com/ArduCAM/Arducam_tof_camera.git");
            System.out.println("    cd Arducam_tof_camera");
            System.out.println("    ./Install_dependencies_raspbian.sh");
            System.out.println();
            System.out.println("  After install, run this test again to verify.");

            logger.log("  NOT FOUND: ~/Arducam_tof_camera");
            logger.log("  Install ArduCam SDK from: https://github.com/ArduCAM/Arducam_tof_camera");
            logger.logFail("ArduCam ToF CAM1", "~/Arducam_tof_camera not found - SDK not installed");
            System.out.println();
            System.out.println("[RESULT] FAIL - ArduCam ToF SDK not installed.");
            System.out.println("  This is expected if you have not installed the SDK yet.");
            System.out.println("  Install the SDK, then re-run this test.");
            return false;
        }

        // SDK folder found - check contents
        System.out.println("  FOUND: " + tofPath);
        logger.log("  FOUND: " + tofPath);

        // Step 2: List demo/example files using find command
        System.out.println();
        System.out.println("[Step 2] Listing demo/example files in " + ARDUCAM_FOLDER + "...");
        logger.log("  Listing example files in ~/Arducam_tof_camera");

        try {
            // Use find to locate Python and C++ examples
            ProcessBuilder pb = new ProcessBuilder(
                "find",
                tofPath.toString(),
                "(", "-name", "*.py", "-o", "-name", "*.cpp", "-o", "-name", "*.sh", ")",
                "-not", "-path", "*/.*"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int foundFiles = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        // Show path relative to home
                        String relative = line.replace(homeDir, "~");
                        System.out.println("  " + relative);
                        logger.log("  File: " + relative);
                        foundFiles++;
                        if (foundFiles >= 20) {
                            System.out.println("  ... (more files not shown)");
                            break;
                        }
                    }
                }
            }
            process.waitFor();

            if (foundFiles == 0) {
                System.out.println("  No .py / .cpp / .sh files found in the SDK folder.");
                System.out.println("  The folder may be empty or SDK install may be incomplete.");
            }

        } catch (Exception e) {
            System.out.println("  Could not list files: " + e.getMessage());
        }

        // Step 3: Check if the dependency install script exists
        Path installScript = tofPath.resolve("Install_dependencies_raspbian.sh");
        if (Files.exists(installScript)) {
            System.out.println();
            System.out.println("[Step 3] Dependency install script found:");
            System.out.println("  " + installScript);
            System.out.println("  If camera is not working, run: bash " + installScript);
            logger.log("  Install script: " + installScript);
        }

        logger.logPass("ArduCam ToF CAM1 SDK Detection");
        System.out.println();
        System.out.println("[RESULT] PASS - ArduCam ToF SDK folder found.");
        System.out.println("  Camera preview was NOT started (detection check only).");
        System.out.println("  Run the ArduCam demo programs manually to test the camera.");
        return true;
    }
}
