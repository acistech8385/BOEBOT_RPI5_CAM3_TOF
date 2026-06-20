package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ToFPreviewTest - Menu Option 11: ArduCam ToF live preview on CAM1.
 *
 * Generates a Python preview script and runs it with python3.
 * Shows depth (JET colourmap) and confidence (greyscale) side by side.
 * The preview window stays open until the user closes it or presses Q/ESC.
 *
 * Requires:
 *   - A display (DISPLAY must be set).
 *   - ArducamDepthCamera Python module installed.
 *   - numpy and opencv-python (cv2) installed.
 *
 * NOTE: Remote Desktop (RDP) does NOT export DISPLAY.
 * Use VNC or a physical screen for live preview.
 * Use option 12 (ToF capture/save) if no display is available.
 */
public class ToFPreviewTest {

    private static final String TOF_PREVIEW_SCRIPT = """
            #!/usr/bin/env python3
            # BOEBOT ArduCam ToF Live Preview
            # API: cam.open(ac.Connection.CSI, 0), frame.depth_data, frame.confidence_data
            import sys
            import time

            def log(msg):
                print("BOEBOT: " + str(msg), flush=True)

            def fail(msg):
                print("FAIL: " + str(msg), flush=True)
                sys.exit(1)

            try:
                import ArducamDepthCamera as ac
            except ImportError as e:
                fail("ArducamDepthCamera not importable: " + str(e) +
                     " | Run: ~/Arducam_tof_camera/Install_dependencies.sh")

            try:
                import numpy as np
            except ImportError:
                fail("numpy not found | Run: pip3 install numpy")

            try:
                import cv2
            except ImportError:
                fail("cv2 not found | Run: sudo apt-get install -y python3-opencv")

            log("Opening ArduCam ToF camera (CSI, port 0)...")
            cam = ac.ArducamCamera()

            ret = cam.open(ac.Connection.CSI, 0)
            if ret != 0:
                fail("Camera open failed. Error code: " + str(ret))

            ret = cam.start(ac.FrameType.DEPTH)
            if ret != 0:
                cam.close()
                fail("Camera start failed. Error code: " + str(ret))

            MAX_DISTANCE = 4000
            try:
                cam.setControl(ac.Control.RANGE, MAX_DISTANCE)
                r = cam.getControl(ac.Control.RANGE)
            except Exception:
                r = MAX_DISTANCE

            try:
                info = cam.getCameraInfo()
                log("Camera: " + str(info.width) + "x" + str(info.height) + ", range: " + str(r) + "mm")
            except Exception:
                log("Camera started. Range: " + str(r) + "mm")

            log("Close the window or press Q / ESC to stop.")
            frames = 0

            while True:
                frame = cam.requestFrame(200)
                if frame is not None and isinstance(frame, ac.DepthData):
                    depth = frame.depth_data
                    confidence = frame.confidence_data
                    cam.releaseFrame(frame)
                    frames += 1

                    result = (depth * (255.0 / r)).astype("uint8")
                    result = cv2.applyColorMap(result, cv2.COLORMAP_JET)
                    result[confidence < 30] = 0

                    conf_norm = cv2.normalize(confidence, None, 0, 255, cv2.NORM_MINMAX).astype("uint8")
                    conf_bgr = cv2.cvtColor(conf_norm, cv2.COLOR_GRAY2BGR)

                    combo = cv2.hconcat([result, conf_bgr])
                    cv2.imshow("ArduCam ToF -- Depth (colour) | Confidence (grey)", combo)

                k = cv2.waitKey(1) & 0xFF
                if k in (ord("q"), ord("Q"), 27):
                    break

            cam.stop()
            cam.close()
            cv2.destroyAllWindows()
            log("Preview closed after " + str(frames) + " frames.")
            """;

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 11: ArduCam ToF Live Preview CAM1");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 1");
        System.out.println("  Type        : Live depth + confidence preview window");

        logger.logSeparator();
        logger.log("TEST 11: ArduCam ToF Live Preview - CAM/DISP 1");

        // ---- Check display ----
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
            System.out.println("  NOTE: Remote Desktop (RDP) does NOT export DISPLAY.");
            System.out.println();
            System.out.println("  Live preview requires one of:");
            System.out.println("    a) Physical HDMI/DSI display connected to the Raspberry Pi.");
            System.out.println("    b) VNC remote desktop with a desktop session.");
            System.out.println("    c) X11 forwarding:  ssh -X faix@boebot-1");
            System.out.println();
            System.out.println("  Use option 12 (ToF capture/save) instead - works without a display.");
            logger.logFail("ArduCam ToF Live Preview", "No display detected");
            System.out.println();
            System.out.println("[RESULT] FAIL - No display for live preview.");
            return false;
        }

        System.out.println("  Display: " + (displayEnv != null ? displayEnv : waylandEnv));

        // ---- Write temp Python script ----
        System.out.println();
        System.out.println("[Step 2] Preparing ToF preview script...");
        Path tmpScript;
        try {
            tmpScript = Files.createTempFile("boebot_tof_preview_", ".py");
            Files.writeString(tmpScript, TOF_PREVIEW_SCRIPT);
            tmpScript.toFile().deleteOnExit();
            System.out.println("  Script: " + tmpScript);
        } catch (Exception e) {
            System.out.println("  [ERROR] Cannot write temp script: " + e.getMessage());
            logger.logFail("ArduCam ToF Live Preview", "temp file error: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        // ---- Launch preview ----
        System.out.println();
        System.out.println("[Step 3] Launching ArduCam ToF live preview...");
        System.out.println("  Command: python3 " + tmpScript);
        System.out.println();
        System.out.println("  >>> Preview window opening. Close it to return to the menu.");
        System.out.println("  >>> Press Q or ESC inside the window to stop.");
        System.out.println("  >>> Or press Ctrl+C in the terminal to force-stop.");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", tmpScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Print Python output in background
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.isBlank()) System.out.println("  [tof] " + line);
                    }
                } catch (Exception ignore) {}
            });
            reader.setDaemon(true);
            reader.start();

            // Give it time to start and detect early failure
            Thread.sleep(800);
            try {
                int earlyExit = process.exitValue();
                System.out.println();
                System.out.println("  [WARNING] ToF preview exited early (exit code " + earlyExit + ").");
                System.out.println("  Check the [tof] lines above for the error.");
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Run: bash ~/Arducam_tof_camera/Install_dependencies.sh");
                System.out.println("  - Run: ./scripts/install_boebot.sh  (installs numpy + cv2 + SDK)");
                System.out.println("  - Check CAM1 ribbon cable connection.");
                System.out.println("  - Run option 10 (SDK detection) to diagnose.");
                logger.logFail("ArduCam ToF Live Preview", "Process exited early. Code: " + earlyExit);
                System.out.println("[RESULT] FAIL - Preview exited unexpectedly.");
                return false;
            } catch (IllegalThreadStateException e) {
                // Still running - good
                System.out.println("  Preview is running...");
            }

            // Block until the user closes the preview window
            int exitCode = process.waitFor();
            System.out.println();
            System.out.println("  Preview window closed (exit code " + exitCode + ").");
            logger.logPass("ArduCam ToF Live Preview CAM1");
            System.out.println();
            System.out.println("[RESULT] PASS - ToF live preview completed.");
            return exitCode == 0;

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("  [ERROR] " + msg);
            logger.logFail("ArduCam ToF Live Preview", msg != null ? msg : "unknown error");
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
