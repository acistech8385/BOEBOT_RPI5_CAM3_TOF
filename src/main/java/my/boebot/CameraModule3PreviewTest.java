package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * CameraModule3PreviewTest - Menu Option 9: Camera Module 3 live preview on CAM0.
 *
 * Opens a live camera preview window.
 * The preview stays open until the user closes the camera window.
 *
 * Tries these commands in order:
 *   1. rpicam-hello  (Raspberry Pi OS Bookworm)
 *   2. libcamera-hello (older Raspberry Pi OS)
 *
 * Requirements:
 *   - A display must be connected (HDMI or DSI), or VNC/X11 forwarding must be active.
 *   - Remote Desktop (RDP) does NOT export DISPLAY — use VNC or a physical screen instead.
 *
 * To close: close the camera window (or press Q if rpicam-hello supports it).
 */
public class CameraModule3PreviewTest {

    private static final String[] PREVIEW_COMMANDS = {"rpicam-hello", "libcamera-hello"};

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 12: Camera Module 3 Live Preview CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Type        : Live preview window");

        logger.logSeparator();
        logger.log("TEST 12: Camera Module 3 Live Preview - CAM/DISP 0");

        // ---- Check for a display ----
        String displayEnv  = System.getenv("DISPLAY");
        String waylandEnv  = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (displayEnv != null && !displayEnv.isEmpty())
                          || (waylandEnv  != null && !waylandEnv.isEmpty());

        System.out.println();
        System.out.println("[Step 1] Checking display environment...");
        if (displayEnv != null) System.out.println("  DISPLAY          = " + displayEnv);
        if (waylandEnv != null) System.out.println("  WAYLAND_DISPLAY  = " + waylandEnv);

        if (!hasDisplay) {
            System.out.println();
            System.out.println("  LIVE PREVIEW NOT AVAILABLE: no display detected. Use still capture test instead.");
            System.out.println();
            System.out.println("  Live preview requires one of:");
            System.out.println("    a) Physical HDMI/DSI display connected to the Raspberry Pi.");
            System.out.println("    b) VNC remote desktop with a desktop session.");
            System.out.println("    c) X11 forwarding:  ssh -X faix@boebot-1");
            System.out.println();
            System.out.println("  NOTE: Remote Desktop (RDP) does NOT export DISPLAY.");
            System.out.println("  Use VNC or a physical screen for live preview.");
            System.out.println();
            System.out.println("  Use option 8 (Camera Module 3 still capture) instead.");
            logger.logFail("Camera Module 3 Preview", "No DISPLAY or WAYLAND_DISPLAY set");
            System.out.println();
            System.out.println("[RESULT] FAIL - No display for live preview.");
            return false;
        }

        System.out.println("  Display detected: " + (displayEnv != null ? displayEnv : waylandEnv));
        logger.log("  Display: " + (displayEnv != null ? displayEnv : waylandEnv));

        // ---- Find preview command ----
        System.out.println();
        System.out.println("[Step 2] Looking for camera preview command...");
        String previewCmd = CameraModule3Test.findCameraCommand(PREVIEW_COMMANDS);

        if (previewCmd == null) {
            System.out.println("  No preview command found (tried: rpicam-hello, libcamera-hello).");
            System.out.println("  Install: sudo apt-get install -y rpicam-apps");
            logger.logFail("Camera Module 3 Preview", "rpicam-hello / libcamera-hello not found");
            System.out.println();
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println("  Using: " + previewCmd);
        logger.log("  Command: " + previewCmd + " --camera " + config.getCameraModule3Port() + " --timeout 0");

        // ---- Start the preview ----
        System.out.println();
        System.out.println("[Step 3] Starting live preview...");
        System.out.println("  Command: " + previewCmd
            + " --camera " + config.getCameraModule3Port() + " --timeout 0");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                previewCmd,
                "--camera", String.valueOf(config.getCameraModule3Port()),
                "--timeout", "0"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read camera process output in background
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) System.out.println("  [camera] " + line);
                    }
                } catch (Exception ignore) {}
            });
            outputReader.setDaemon(true);
            outputReader.start();

            // Give it a moment to start
            Thread.sleep(500);

            // Check for early exit
            try {
                int earlyExit = process.exitValue();
                System.out.println("  [WARNING] Camera process exited early (exit code " + earlyExit + ").");
                System.out.println();
                System.out.println("  POSSIBLE CAUSES:");
                System.out.println("  - No camera detected on CAM0. Check the ribbon cable.");
                System.out.println("  - Camera not enabled: sudo raspi-config -> Interface Options -> Camera.");
                System.out.println("  - Check: " + previewCmd + " --list-cameras");
                logger.logFail("Camera Module 3 Preview", "Process exited early. Exit: " + earlyExit);
                System.out.println();
                System.out.println("[RESULT] FAIL - Preview process exited unexpectedly.");
                return false;
            } catch (IllegalThreadStateException e) {
                // Good - process is still running
            }

            System.out.println("  >>> Camera preview window is now open.");
            System.out.println("  >>> Close the camera window to return to the menu.");
            System.out.println("  >>> Or press Ctrl+C to force-stop.");
            System.out.println();

            // Block until the camera window is closed
            int exitCode = process.waitFor();

            System.out.println("  Preview stopped (exit code " + exitCode + ").");
            logger.logPass("Camera Module 3 Live Preview CAM0");
            System.out.println();
            System.out.println("[RESULT] PASS - Camera Module 3 live preview completed.");
            return true;

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("  [ERROR] " + msg);
            logger.logFail("Camera Module 3 Preview", msg != null ? msg : "Unknown error");
            System.out.println();
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
