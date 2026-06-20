package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * CameraModule3PreviewTest - Menu Option 9: Camera Module 3 live preview on CAM0.
 *
 * Opens a live camera preview window on screen.
 * The preview stays open until the user presses Enter in the terminal.
 *
 * Tries these commands in order:
 *   1. rpicam-hello  (Raspberry Pi OS Bookworm)
 *   2. libcamera-hello (older Raspberry Pi OS)
 *
 * Requirements:
 *   - A display must be connected (HDMI or DSI).
 *   - The Raspberry Pi must be booted to a desktop environment, OR
 *     you must be running via VNC/remote desktop.
 *
 * If no display is detected (headless SSH session with no DISPLAY variable),
 * the preview cannot be shown and this test will offer the still capture instead.
 *
 * To close the preview: press Enter in this terminal.
 */
public class CameraModule3PreviewTest {

    // Try these preview commands in order (Bookworm first, then legacy)
    private static final String[] PREVIEW_COMMANDS = {"rpicam-hello", "libcamera-hello"};

    public static boolean run(AppLogger logger, BotConfig config, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 9: Camera Module 3 Live Preview CAM0");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 0");
        System.out.println("  Type        : Live preview window");

        logger.logSeparator();
        logger.log("TEST 9: Camera Module 3 Live Preview - CAM/DISP 0");

        // ---- Check for a display ----
        String displayEnv    = System.getenv("DISPLAY");
        String waylandEnv    = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay   = (displayEnv != null && !displayEnv.isEmpty())
                            || (waylandEnv  != null && !waylandEnv.isEmpty());

        System.out.println();
        System.out.println("[Step 1] Checking for display environment...");
        if (displayEnv != null)  System.out.println("  DISPLAY          = " + displayEnv);
        if (waylandEnv != null)  System.out.println("  WAYLAND_DISPLAY  = " + waylandEnv);

        if (!hasDisplay) {
            System.out.println();
            System.out.println("  No display detected (DISPLAY / WAYLAND_DISPLAY not set).");
            System.out.println();
            System.out.println("  This usually means you are connected via SSH without a");
            System.out.println("  display forwarding session.");
            System.out.println();
            System.out.println("  Live preview requires one of:");
            System.out.println("    a) A physical HDMI/DSI display connected to the Raspberry Pi.");
            System.out.println("    b) VNC or RDP remote desktop with a desktop session.");
            System.out.println("    c) X11 forwarding:  ssh -X pi@<hostname>");
            System.out.println();
            System.out.println("  ALTERNATIVE: Use menu option 8 (Camera Module 3 still capture)");
            System.out.println("  to test the camera without a display.");

            logger.log("  No display detected. Live preview not possible.");
            logger.logFail("Camera Module 3 Preview", "No DISPLAY or WAYLAND_DISPLAY set - headless session");
            System.out.println();
            System.out.println("[RESULT] FAIL - No display available for live preview.");
            System.out.println("  Use option 8 (still capture) to test the camera instead.");
            return false;
        }

        System.out.println("  Display detected. Proceeding with preview.");
        logger.log("  Display: " + (displayEnv != null ? displayEnv : waylandEnv));

        // ---- Find preview command ----
        System.out.println();
        System.out.println("[Step 2] Looking for camera preview command...");
        String previewCmd = CameraModule3Test.findCameraCommand(PREVIEW_COMMANDS);

        if (previewCmd == null) {
            System.out.println("  No preview command found.");
            System.out.println("  Tried: rpicam-hello, libcamera-hello");
            System.out.println();
            System.out.println("  Install camera tools:");
            System.out.println("    sudo apt-get install -y rpicam-apps");
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
        System.out.println();
        System.out.println("  Command: " + previewCmd
            + " --camera " + config.getCameraModule3Port() + " --timeout 0");
        System.out.println();
        System.out.println("  >>> The camera preview window should open on the screen.");
        System.out.println("  >>> Preview runs indefinitely (--timeout 0).");
        System.out.println("  >>> Press ENTER in this terminal window to STOP the preview.");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                previewCmd,
                "--camera", String.valueOf(config.getCameraModule3Port()),
                "--timeout", "0"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read camera process output in a background thread so it doesn't block
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            System.out.println("  [camera] " + line);
                        }
                    }
                } catch (Exception ignore) {
                    // Process was stopped - expected
                }
            });
            outputReader.setDaemon(true);
            outputReader.start();

            // Give the camera a moment to start
            Thread.sleep(500);

            // Check if process is still running (it should be for a live preview)
            try {
                int earlyExit = process.exitValue();
                // If we get here, the process already exited
                System.out.println("  [WARNING] Camera process exited early (exit code " + earlyExit + ").");
                System.out.println();
                System.out.println("  POSSIBLE CAUSES:");
                System.out.println("  - No camera detected on CAM0. Check the ribbon cable.");
                System.out.println("  - Camera not enabled: sudo raspi-config -> Interface Options -> Camera.");
                System.out.println("  - Check: " + previewCmd + " --list-cameras");
                logger.logFail("Camera Module 3 Preview", "Process exited early. Exit code: " + earlyExit);
                System.out.println();
                System.out.println("[RESULT] FAIL - Preview process exited unexpectedly.");
                return false;
            } catch (IllegalThreadStateException e) {
                // Good - process is still running (this is expected)
                System.out.println("  Preview is running.");
            }

            // Wait for user to press Enter
            System.out.print("  Press ENTER to stop preview: ");
            scanner.nextLine();

            // Stop the camera process
            System.out.println("  Stopping preview...");
            process.destroy();
            try {
                process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {}

            System.out.println("  Preview stopped.");
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
