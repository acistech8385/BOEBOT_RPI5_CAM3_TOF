package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * CameraModule3Test - Menu Option 8: Test Raspberry Pi Camera Module 3 on CAM0.
 *
 * Uses the rpicam-still command to capture a test image.
 * Image is saved to: logs/<hostname>/cam3_test.jpg
 *
 * PASS = image file created successfully.
 * FAIL = command not found or image not created.
 *
 * If rpicam-still is not found, prints instructions to install rpicam-apps.
 */
public class CameraModule3Test {

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 8: Camera Module 3 CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Command     : rpicam-still --camera 0");

        logger.logSeparator();
        logger.log("TEST 8: Camera Module 3 - CAM/DISP 0");

        // Build the output file path in logs/<hostname>/
        String hostname = logger.getHostname();
        String logDir = "logs" + File.separator + hostname;
        String imageFile = logDir + File.separator + "cam3_test.jpg";

        System.out.println("  Output file : " + imageFile);
        logger.log("  Output file : " + imageFile);

        // Make sure the log directory exists
        new File(logDir).mkdirs();

        // Delete any previous test image so we can verify a new one is created
        File outputFile = new File(imageFile);
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // Build the rpicam-still command
        // --camera 0 = Camera Module 3 on CAM/DISP 0
        // --timeout 2000 = capture after 2 seconds warmup
        // -o = output file path
        ProcessBuilder pb = new ProcessBuilder(
            "rpicam-still",
            "--camera", String.valueOf(config.getCameraModule3Port()),
            "-o", imageFile,
            "--timeout", "2000"
        );
        pb.redirectErrorStream(true);

        System.out.println();
        System.out.println("[Step 1] Running: rpicam-still --camera "
            + config.getCameraModule3Port() + " -o " + imageFile + " --timeout 2000");
        System.out.println("  Please wait (approx 3 seconds)...");

        try {
            Process process = pb.start();

            // Collect any output/errors from the command
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Print camera output to console
                    if (!line.isBlank()) {
                        System.out.println("  [camera] " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            logger.log("  rpicam-still exit code: " + exitCode);

            // Check if the image file was created
            if (outputFile.exists() && outputFile.length() > 0) {
                long sizeKB = outputFile.length() / 1024;
                System.out.println();
                System.out.println("[Step 2] Image file created: " + imageFile);
                System.out.println("  File size: " + sizeKB + " KB");
                logger.log("  Image created: " + imageFile + " (" + sizeKB + " KB)");
                logger.logPass("Camera Module 3 CAM0");
                System.out.println();
                System.out.println("[RESULT] PASS - Camera Module 3 captured test image.");
                return true;
            } else {
                System.out.println();
                System.out.println("[Step 2] Image file NOT created.");
                logger.log("  Image not created. Exit code: " + exitCode);

                if (output.toString().contains("not found") || exitCode == 127) {
                    printInstallInstructions();
                } else {
                    System.out.println("  Camera output: " + output.toString().trim());
                    System.out.println();
                    System.out.println("  POSSIBLE FIXES:");
                    System.out.println("  - Check camera cable connection to CAM/DISP 0.");
                    System.out.println("  - Check camera is enabled: sudo raspi-config -> Interface Options.");
                    System.out.println("  - Try manually: rpicam-still -o /tmp/test.jpg --timeout 2000");
                }

                logger.logFail("Camera Module 3 CAM0", "Image file not created");
                System.out.println();
                System.out.println("[RESULT] FAIL - Camera Module 3 test failed.");
                return false;
            }

        } catch (Exception e) {
            String msg = e.getMessage();

            if (msg != null && (msg.contains("No such file") || msg.contains("Cannot run"))) {
                System.out.println();
                System.out.println("[ERROR] rpicam-still command not found.");
                printInstallInstructions();
            } else {
                System.out.println("[ERROR] " + msg);
            }

            logger.logFail("Camera Module 3 CAM0", msg != null ? msg : "Unknown error");
            System.out.println();
            System.out.println("[RESULT] FAIL - Camera Module 3 test failed.");
            return false;
        }
    }

    private static void printInstallInstructions() {
        System.out.println();
        System.out.println("  rpicam-still NOT FOUND on this system.");
        System.out.println();
        System.out.println("  To install rpicam-apps:");
        System.out.println("    sudo apt-get update");
        System.out.println("    sudo apt-get install -y rpicam-apps");
        System.out.println();
        System.out.println("  If that fails, try:");
        System.out.println("    sudo apt-get install -y libcamera-apps");
        System.out.println();
        System.out.println("  After install, check the camera is detected:");
        System.out.println("    rpicam-still --list-cameras");
    }
}
