package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DualCameraViewTest - Menu Option 14: live view of BOTH cameras at once.
 *
 * Shows, side by side in one OpenCV window:
 *   - Left  : Camera Module 3 RGB feed   (CAM0, imx708, via picamera2)
 *   - Right : ArduCam ToF depth map       (CAM1, JET colourmap, via SDK)
 *
 * Proves both cameras run simultaneously without conflict.
 *
 * Requires:
 *   - A display (DISPLAY / WAYLAND_DISPLAY). RDP does NOT export DISPLAY.
 *   - python3-picamera2  (RGB camera).
 *   - ArducamDepthCamera, numpy, opencv-python (ToF + display).
 *
 * The ToF open() index is the /dev/videoN node, not the CSI port.
 * With Camera Module 3 on CAM0 (video0-7) the ToF lands on video8, so the
 * script probes [8, 0, 9, 10, 11, 12] and uses the first node that opens.
 */
public class DualCameraViewTest {

    private static final String DUAL_VIEW_SCRIPT = """
            #!/usr/bin/env python3
            # BOEBOT Dual Camera Live View
            # Left  = Camera Module 3 RGB (CAM0, picamera2)
            # Right = ArduCam ToF depth   (CAM1, ArducamDepthCamera, index = /dev/videoN)
            import sys
            import time

            def log(msg):
                print("BOEBOT: " + str(msg), flush=True)

            def fail(msg):
                print("FAIL: " + str(msg), flush=True)
                sys.exit(1)

            try:
                import numpy as np
            except ImportError:
                fail("numpy not found | Run: pip3 install numpy")

            try:
                import cv2
            except ImportError:
                fail("cv2 not found | Run: sudo apt-get install -y python3-opencv")

            try:
                from picamera2 import Picamera2
            except ImportError as e:
                fail("picamera2 not importable: " + str(e) +
                     " | Run: sudo apt-get install -y python3-picamera2")

            try:
                import ArducamDepthCamera as ac
            except ImportError as e:
                fail("ArducamDepthCamera not importable: " + str(e))

            VIEW_W, VIEW_H = 480, 360

            # -- Open Camera Module 3 (RGB) on CAM0 --
            log("Opening Camera Module 3 (RGB, CAM0)...")
            try:
                picam2 = Picamera2(0)
                cfg = picam2.create_video_configuration(
                    main={"size": (VIEW_W, VIEW_H), "format": "RGB888"})
                picam2.configure(cfg)
                picam2.start()
            except Exception as e:
                fail("Camera Module 3 open failed: " + str(e))

            # -- Open ArduCam ToF (depth) on CAM1 --
            # index = /dev/videoN node. ToF on CAM1 = video8 alongside Camera Module 3.
            log("Opening ArduCam ToF (depth, CAM1)...")
            cam = ac.ArducamCamera()
            candidates = [8, 0, 9, 10, 11, 12]
            opened_index = None
            for idx in candidates:
                if cam.open(ac.Connection.CSI, idx) == 0:
                    opened_index = idx
                    log("ToF opened on /dev/video" + str(idx))
                    break
            if opened_index is None:
                picam2.stop()
                fail("ToF open failed on indices " + str(candidates))

            if cam.start(ac.FrameType.DEPTH) != 0:
                cam.close()
                picam2.stop()
                fail("ToF start failed")

            MAX_DISTANCE = 4000
            try:
                cam.setControl(ac.Control.RANGE, MAX_DISTANCE)
            except Exception:
                pass
            r = MAX_DISTANCE

            log("Both cameras running. Press Q / ESC in the window to stop.")
            frames = 0
            depth_vis = np.zeros((VIEW_H, VIEW_W, 3), dtype="uint8")

            while True:
                # -- RGB frame from Camera Module 3 --
                # picamera2 "RGB888" already returns BGR byte order, which is
                # what OpenCV imshow expects -- do NOT cvtColor or skin goes blue.
                rgb = picam2.capture_array()
                if rgb.shape[1] != VIEW_W or rgb.shape[0] != VIEW_H:
                    rgb = cv2.resize(rgb, (VIEW_W, VIEW_H))

                # -- Depth frame from ToF --
                frame = cam.requestFrame(200)
                if frame is not None and isinstance(frame, ac.DepthData):
                    depth = frame.depth_data
                    confidence = frame.confidence_data
                    cam.releaseFrame(frame)
                    d = np.clip(depth, 0, r)
                    d8 = (d * (255.0 / r)).astype("uint8")
                    depth_vis = cv2.applyColorMap(d8, cv2.COLORMAP_JET)
                    depth_vis[confidence < 30] = 0
                    depth_vis = cv2.resize(depth_vis, (VIEW_W, VIEW_H),
                                           interpolation=cv2.INTER_NEAREST)

                left = rgb.copy()
                right = depth_vis.copy()
                cv2.putText(left, "Camera Module 3 (RGB) CAM0", (8, 22),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 255, 0), 2)
                cv2.putText(right, "ArduCam ToF (Depth) CAM1", (8, 22),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 255, 255), 2)

                combo = cv2.hconcat([left, right])
                cv2.imshow("BOEBOT Dual View -- RGB (CAM0) | Depth (CAM1)", combo)
                frames += 1

                k = cv2.waitKey(1) & 0xFF
                if k in (ord("q"), ord("Q"), 27):
                    break

            cam.stop()
            cam.close()
            picam2.stop()
            cv2.destroyAllWindows()
            log("Dual view closed after " + str(frames) + " frames.")
            """;

    /**
     * Write the dual-view script to a temp file and start it as a background
     * python3 process (no wait). Returns the Process, or null on failure.
     * Used by the full test (option 16) to show both cameras while driving.
     */
    public static Process launchBackground() {
        try {
            Path tmpScript = Files.createTempFile("boebot_dual_view_", ".py");
            Files.writeString(tmpScript, DUAL_VIEW_SCRIPT);
            tmpScript.toFile().deleteOnExit();
            ProcessBuilder pb = new ProcessBuilder("python3", tmpScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.isBlank()) System.out.println("  [dual] " + line);
                    }
                } catch (Exception ignore) {}
            });
            reader.setDaemon(true);
            reader.start();
            return process;
        } catch (Exception e) {
            System.out.println("  [WARN] Could not start dual camera view: " + e.getMessage());
            return null;
        }
    }

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 13: Dual Camera Live View");
        System.out.println("====================================");
        System.out.println("  Left  : Camera Module 3 RGB  (CAM0)");
        System.out.println("  Right : ArduCam ToF depth     (CAM1)");
        System.out.println("  Type  : Both cameras in one window");

        logger.logSeparator();
        logger.log("TEST 14: Dual Camera Live View - CAM0 (RGB) + CAM1 (ToF)");

        // ---- Check display ----
        String displayEnv = System.getenv("DISPLAY");
        String waylandEnv = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (displayEnv != null && !displayEnv.isEmpty())
                          || (waylandEnv != null && !waylandEnv.isEmpty());

        System.out.println();
        System.out.println("[Step 1] Checking display environment...");
        if (displayEnv != null) System.out.println("  DISPLAY          = " + displayEnv);
        if (waylandEnv != null) System.out.println("  WAYLAND_DISPLAY  = " + waylandEnv);

        if (!hasDisplay) {
            System.out.println();
            System.out.println("  DUAL VIEW NOT AVAILABLE: no display detected.");
            System.out.println("  NOTE: Remote Desktop (RDP) does NOT export DISPLAY.");
            System.out.println();
            System.out.println("  Dual view requires one of:");
            System.out.println("    a) Physical HDMI/DSI display connected to the Raspberry Pi.");
            System.out.println("    b) VNC remote desktop with a desktop session.");
            System.out.println("    c) X11 forwarding:  ssh -X faix@boebot-1");
            System.out.println();
            System.out.println("  Use option 8 (CM3 capture) + option 12 (ToF capture) instead.");
            logger.logFail("Dual Camera View", "No display detected");
            System.out.println();
            System.out.println("[RESULT] FAIL - No display for dual view.");
            return false;
        }

        System.out.println("  Display: " + (displayEnv != null ? displayEnv : waylandEnv));

        // ---- Write temp Python script ----
        System.out.println();
        System.out.println("[Step 2] Preparing dual view script...");
        Path tmpScript;
        try {
            tmpScript = Files.createTempFile("boebot_dual_view_", ".py");
            Files.writeString(tmpScript, DUAL_VIEW_SCRIPT);
            tmpScript.toFile().deleteOnExit();
            System.out.println("  Script: " + tmpScript);
        } catch (Exception e) {
            System.out.println("  [ERROR] Cannot write temp script: " + e.getMessage());
            logger.logFail("Dual Camera View", "temp file error: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        // ---- Launch dual view ----
        System.out.println();
        System.out.println("[Step 3] Launching dual camera live view...");
        System.out.println("  Command: python3 " + tmpScript);
        System.out.println();
        System.out.println("  >>> Dual view window opening. Close it to return to the menu.");
        System.out.println("  >>> Press Q or ESC inside the window to stop.");
        System.out.println("  >>> Or press Ctrl+C in the terminal to force-stop.");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", tmpScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.isBlank()) System.out.println("  [dual] " + line);
                    }
                } catch (Exception ignore) {}
            });
            reader.setDaemon(true);
            reader.start();

            // Give it time to start and detect early failure
            Thread.sleep(1500);
            try {
                int earlyExit = process.exitValue();
                System.out.println();
                System.out.println("  [WARNING] Dual view exited early (exit code " + earlyExit + ").");
                System.out.println("  Check the [dual] lines above for the error.");
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Run: sudo apt-get install -y python3-picamera2 python3-opencv");
                System.out.println("  - Run: bash ~/Arducam_tof_camera/Install_dependencies.sh");
                System.out.println("  - Check CAM0 (Camera Module 3) and CAM1 (ToF) ribbon cables.");
                System.out.println("  - Run option 9 (CM3) and option 11 (ToF) separately to isolate.");
                logger.logFail("Dual Camera View", "Process exited early. Code: " + earlyExit);
                System.out.println("[RESULT] FAIL - Dual view exited unexpectedly.");
                return false;
            } catch (IllegalThreadStateException e) {
                System.out.println("  Dual view is running...");
            }

            int exitCode = process.waitFor();
            System.out.println();
            System.out.println("  Dual view window closed (exit code " + exitCode + ").");
            System.out.println();
            if (exitCode == 0) {
                logger.logPass("Dual Camera Live View (CAM0 RGB + CAM1 ToF)");
                System.out.println("[RESULT] PASS - Dual camera live view completed.");
            } else {
                System.out.println("  Dual view exited with error. Check [dual] output above.");
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Run: sudo apt-get install -y python3-picamera2 python3-opencv");
                System.out.println("  - Run: bash ~/Arducam_tof_camera/Install_dependencies.sh");
                System.out.println("  - Run option 9 (CM3) and option 11 (ToF) separately to isolate.");
                logger.logFail("Dual Camera View", "Python exited with code " + exitCode);
                System.out.println("[RESULT] FAIL - Dual view exited with error.");
            }
            return exitCode == 0;

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("  [ERROR] " + msg);
            logger.logFail("Dual Camera View", msg != null ? msg : "unknown error");
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
