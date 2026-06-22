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
 *   logs/<hostname>/cam3_YYYYMMDD_HHMMSS.jpg
 *
 * PASS = image file created successfully.
 * FAIL = command not found or image not created.
 */
public class CameraModule3Test {

    private static final String[] STILL_COMMANDS = {"rpicam-still", "libcamera-still"};

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 11: Camera Module 3 Still Capture CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Type        : Still capture (jpg)");

        logger.logSeparator();
        logger.log("TEST 11: Camera Module 3 Still Capture - CAM/DISP 0");

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

        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String hostname  = logger.getHostname();
        String fileName  = "cam3_" + timestamp + ".jpg";
        File   logDir    = new File("logs" + File.separator + hostname).getAbsoluteFile();
        File   imageFile = new File(logDir, fileName);

        logDir.mkdirs();

        logger.log("  Command  : " + cameraCmd);
        logger.log("  Output   : " + imageFile.getAbsolutePath());

        System.out.println();
        System.out.println("[Step 1] Running: " + cameraCmd
            + " --camera " + config.getCameraModule3Port()
            + " -o " + imageFile.getPath() + " --timeout 2000");
        System.out.println("  Please wait (approx 3 seconds)...");

        try {
            ProcessBuilder pb = new ProcessBuilder(
                cameraCmd,
                "--camera", String.valueOf(config.getCameraModule3Port()),
                "-o", imageFile.getAbsolutePath(),
                "--timeout", "2000"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (!line.isBlank()) System.out.println("  [camera] " + line);
                }
            }

            int exitCode = process.waitFor();
            logger.log("  Exit code: " + exitCode);

            if (imageFile.exists() && imageFile.length() > 0) {
                long sizeKB = imageFile.length() / 1024;
                System.out.println();
                System.out.println("IMAGE SAVED:");
                System.out.println("Folder:    " + logDir.getAbsolutePath());
                System.out.println("File:      " + fileName);
                System.out.println("Full path: " + imageFile.getAbsolutePath());
                System.out.println("Size:      " + sizeKB + " KB");
                logger.log("  Image saved: " + imageFile.getAbsolutePath() + " (" + sizeKB + " KB)");
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
                p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                if (p.waitFor() == 0) return cmd;
            } catch (Exception ignore) {}
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
