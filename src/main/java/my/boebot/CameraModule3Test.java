package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CameraModule3Test - Menu Option 8: Camera Module 3 still capture on CAM0.
 *
 * Tries these commands in order:
 *   1. rpicam-still  (Raspberry Pi OS Bookworm)
 *   2. libcamera-still (older Raspberry Pi OS)
 *
 * Saves a timestamped image to:
 *   logs/<hostname>/cam3_still_<timestamp>.jpg
 *
 * PASS = image file created successfully.
 * FAIL = command not found or image not created.
 */
public class CameraModule3Test {

    // Try these camera commands in order (Bookworm first, then legacy)
    private static final String[] STILL_COMMANDS = {"rpicam-still", "libcamera-still"};

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 8: Camera Module 3 Still Capture CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Type        : Still capture (jpg)");

        logger.logSeparator();
        logger.log("TEST 8: Camera Module 3 Still Capture - CAM/DISP 0");

        // Find which camera command is available
        String cameraCmd = findCameraCommand(STILL_COMMANDS);

        if (cameraCmd == null) {
            System.out.println();
            System.out.println("[FAIL] No still capture command found.");
            System.out.println("       Tried: rpicam-still, libcamera-still");
            printInstallInstructions();
            logger.logFail("Camera Module 3 Still", "rpicam-still / libcamera-still not found");
            System.out.println();
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println("  Using : " + cameraCmd);

        // Build a timestamped output filename
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String hostname = logger.getHostname();
        String logDir   = "logs" + File.separator + hostname;
        String imageFile = logDir + File.separator + "cam3_still_" + timestamp + ".jpg";

        new File(logDir).mkdirs();

        System.out.println("  Output: " + imageFile);
        logger.log("  Command  : " + cameraCmd);
        logger.log("  Output   : " + imageFile);

        System.out.println();
        System.out.println("[Step 1] Running: " + cameraCmd
            + " --camera " + config.getCameraModule3Port()
            + " -o " + imageFile + " --timeout 2000");
        System.out.println("  Please wait (approx 3 seconds)...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                cameraCmd,
                "--camera", String.valueOf(config.getCameraModule3Port()),
                "-o", imageFile,
                "--timeout", "2000"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read and display any camera output/errors
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (!line.isBlank()) {
                        System.out.println("  [camera] " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            logger.log("  Exit code: " + exitCode);

            // Check if image file was created
            File outputFile = new File(imageFile);
            if (outputFile.exists() && outputFile.length() > 0) {
                long sizeKB = outputFile.length() / 1024;
                System.out.println();
                System.out.println("[Step 2] Image saved successfully.");
                System.out.println("  Path : " + imageFile);
                System.out.println("  Size : " + sizeKB + " KB");
                logger.log("  Image saved: " + imageFile + " (" + sizeKB + " KB)");
                logger.logPass("Camera Module 3 Still Capture CAM0");
                System.out.println();
                System.out.println("[RESULT] PASS - Camera Module 3 captured image.");
                return true;

            } else {
                System.out.println();
                System.out.println("[Step 2] Image file NOT created.");
                System.out.println("  Exit code : " + exitCode);
                if (!output.toString().isBlank()) {
                    System.out.println("  Output    : " + output.toString().trim());
                }
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Check the camera ribbon cable is seated in CAM/DISP 0.");
                System.out.println("  - Enable camera interface: sudo raspi-config -> Interface Options -> Camera.");
                System.out.println("  - Check camera is detected: " + cameraCmd + " --list-cameras");
                logger.logFail("Camera Module 3 Still", "Image not created. Exit: " + exitCode);
                System.out.println();
                System.out.println("[RESULT] FAIL");
                return false;
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println();
            System.out.println("[ERROR] " + msg);
            if (msg != null && (msg.contains("No such file") || msg.contains("Cannot run"))) {
                printInstallInstructions();
            }
            logger.logFail("Camera Module 3 Still", msg != null ? msg : "Unknown error");
            System.out.println();
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }

    /** Finds the first available camera command from the given candidates. */
    static String findCameraCommand(String[] candidates) {
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder("which", cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                // Drain output so the process doesn't hang
                p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (p.waitFor() == 0) {
                    return cmd;
                }
            } catch (Exception ignore) {
                // 'which' not available or other error - try next
            }
        }
        return null;
    }

    private static void printInstallInstructions() {
        System.out.println();
        System.out.println("  To install camera tools on Raspberry Pi OS Bookworm:");
        System.out.println("    sudo apt-get update");
        System.out.println("    sudo apt-get install -y rpicam-apps");
        System.out.println();
        System.out.println("  For older Raspberry Pi OS (Bullseye):");
        System.out.println("    sudo apt-get install -y libcamera-apps");
        System.out.println();
        System.out.println("  Check installed cameras:");
        System.out.println("    rpicam-still --list-cameras");
    }
}
