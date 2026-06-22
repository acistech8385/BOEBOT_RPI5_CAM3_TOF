package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * CameraModule3DetectTest - Menu Option 10: detect Camera Module 3 on CAM0.
 *
 * Runs `rpicam-hello --list-cameras` (or libcamera-hello) and checks that the
 * imx708 sensor (Camera Module 3) is listed. No image is captured and no
 * preview window is opened. PASS = camera detected, FAIL = not found.
 */
public class CameraModule3DetectTest {

    private static final String[] HELLO_COMMANDS = {"rpicam-hello", "libcamera-hello"};

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 10: Camera Module 3 Detection CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Type        : Detection only (no capture, no window)");

        logger.logSeparator();
        logger.log("TEST 10: Camera Module 3 Detection - CAM/DISP 0");

        String cmd = CameraModule3Test.findCameraCommand(HELLO_COMMANDS);
        if (cmd == null) {
            System.out.println("  No camera tool found (tried: rpicam-hello, libcamera-hello).");
            System.out.println("  Install: sudo apt-get install -y rpicam-apps");
            logger.logFail("Camera Module 3 Detection", "rpicam-hello / libcamera-hello not found");
            System.out.println();
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println();
        System.out.println("[Step 1] Running: " + cmd + " --list-cameras");
        System.out.println();

        boolean foundImx708 = false;
        boolean foundAnyCamera = false;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "--list-cameras");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("  [cam] " + line);
                    String low = line.toLowerCase();
                    if (low.contains("imx708")) foundImx708 = true;
                    if (low.contains("available cameras") || low.matches("\\s*\\d+\\s*:.*")) {
                        foundAnyCamera = true;
                    }
                }
            }
            process.waitFor();

        } catch (Exception e) {
            System.out.println("  [ERROR] " + e.getMessage());
            logger.logFail("Camera Module 3 Detection", e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println();
        if (foundImx708) {
            logger.logPass("Camera Module 3 Detection CAM0 (imx708)");
            System.out.println("[RESULT] PASS - Camera Module 3 (imx708) detected on CAM0.");
            return true;
        } else if (foundAnyCamera) {
            System.out.println("  A camera was listed, but not the imx708 (Camera Module 3).");
            System.out.println("  Check that Camera Module 3 is on CAM0 and config.txt has dtoverlay=imx708,cam0.");
            logger.logFail("Camera Module 3 Detection", "imx708 not in camera list");
            System.out.println("[RESULT] FAIL - imx708 not detected.");
            return false;
        } else {
            System.out.println("  No cameras detected.");
            System.out.println("  - Check the CAM0 ribbon cable.");
            System.out.println("  - Check config.txt: camera_auto_detect=0 + dtoverlay=imx708,cam0");
            System.out.println("  - Reboot after config changes.");
            logger.logFail("Camera Module 3 Detection", "no cameras listed");
            System.out.println("[RESULT] FAIL - No camera detected.");
            return false;
        }
    }
}
