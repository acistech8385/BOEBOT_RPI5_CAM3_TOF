package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ToFCaptureTest - Menu Option 12: ArduCam ToF capture and save on CAM1.
 *
 * Captures a single ToF frame and saves:
 *   - tof_depth_YYYYMMDD_HHMMSS.csv        (raw depth values in mm, always saved)
 *   - tof_depth_YYYYMMDD_HHMMSS.png        (depth as JET colourmap, if cv2 available)
 *   - tof_confidence_YYYYMMDD_HHMMSS.png   (confidence greyscale, if cv2 available)
 *
 * Files are saved to: logs/<hostname>/
 *
 * Works without a display - no GUI window is opened.
 *
 * Requires:
 *   - ArducamDepthCamera Python module installed.
 *   - numpy Python module installed.
 *   - opencv-python (cv2) optional but recommended for PNG output.
 */
public class ToFCaptureTest {

    private static final String TOF_CAPTURE_SCRIPT = """
            #!/usr/bin/env python3
            # BOEBOT ArduCam ToF Capture
            # API: cam.open(ac.Connection.CSI, N) — N = /dev/videoN node, ToF=video8 on CAM1
            import sys
            import os
            import time

            save_dir  = sys.argv[1] if len(sys.argv) > 1 else "."
            timestamp = sys.argv[2] if len(sys.argv) > 2 else time.strftime("%Y%m%d_%H%M%S")
            os.makedirs(save_dir, exist_ok=True)
            saved = []

            def log(msg):
                print("BOEBOT: " + str(msg), flush=True)

            def fail(msg):
                print("FAIL: " + str(msg), flush=True)
                sys.exit(1)

            try:
                import ArducamDepthCamera as ac
            except ImportError as e:
                fail("ArducamDepthCamera not importable: " + str(e))

            try:
                import numpy as np
            except ImportError:
                fail("numpy not found | Run: pip3 install numpy")

            try:
                import cv2
                have_cv2 = True
            except ImportError:
                have_cv2 = False
                log("cv2 not available - PNG output skipped, CSV will still save")

            # NOTE: open() index = /dev/videoN number, NOT the CSI port.
            # Camera Module 3 (imx708) on CAM0 takes /dev/video0-7.
            # ArduCam ToF on CAM1 takes /dev/video8-15, so index 8 in a
            # dual-camera setup. If the ToF is the only camera it is video0.
            # Probe candidates and use the first that opens successfully.
            log("Opening ArduCam ToF camera (probing /dev/video nodes)...")
            cam = ac.ArducamCamera()

            candidates = [8, 0, 9, 10, 11, 12]
            ret = None
            opened_index = None
            for idx in candidates:
                ret = cam.open(ac.Connection.CSI, idx)
                if ret == 0:
                    opened_index = idx
                    log("ToF camera opened on /dev/video" + str(idx))
                    break
            if opened_index is None:
                fail("Camera open failed on indices " + str(candidates) +
                     ". Last error: " + str(ret))

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

            log("Warming up...")
            time.sleep(0.5)

            frame = cam.requestFrame(2000)
            if frame is None or not isinstance(frame, ac.DepthData):
                cam.stop()
                cam.close()
                fail("Failed to capture depth frame")

            depth = frame.depth_data
            confidence = frame.confidence_data
            cam.releaseFrame(frame)
            cam.stop()
            cam.close()
            log("Frame captured.")

            # -- Save depth as CSV (raw values in mm) --
            csv_path = os.path.join(save_dir, "tof_depth_" + timestamp + ".csv")
            np.savetxt(csv_path, depth, delimiter=",", fmt="%.0f")
            saved.append(csv_path)
            print("SAVED:" + csv_path, flush=True)

            if have_cv2:
                # Depth as JET colour PNG
                result = (depth * (255.0 / r)).astype("uint8")
                d_png = os.path.join(save_dir, "tof_depth_" + timestamp + ".png")
                cv2.imwrite(d_png, cv2.applyColorMap(result, cv2.COLORMAP_JET))
                saved.append(d_png)
                print("SAVED:" + d_png, flush=True)

                # Confidence as greyscale PNG
                c_n = cv2.normalize(confidence, None, 0, 255, cv2.NORM_MINMAX, cv2.CV_8U)
                c_png = os.path.join(save_dir, "tof_confidence_" + timestamp + ".png")
                cv2.imwrite(c_png, c_n)
                saved.append(c_png)
                print("SAVED:" + c_png, flush=True)

            print("DONE:" + str(len(saved)), flush=True)
            """;

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 15: ArduCam ToF Capture/Save CAM1");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 1");
        System.out.println("  Type        : Single frame capture, save to logs/");
        System.out.println("  Display     : NOT required (no GUI window)");

        logger.logSeparator();
        logger.log("TEST 15: ArduCam ToF Capture/Save - CAM/DISP 1");

        // ---- Prepare output directory ----
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String hostname = logger.getHostname();
        File logDir = new File("logs" + File.separator + hostname).getAbsoluteFile();
        logDir.mkdirs();

        System.out.println();
        System.out.println("[Step 1] Output folder: " + logDir.getAbsolutePath());
        logger.log("  Output dir: " + logDir.getAbsolutePath());

        // ---- Write temp Python script ----
        System.out.println();
        System.out.println("[Step 2] Preparing ToF capture script...");
        Path tmpScript;
        try {
            tmpScript = Files.createTempFile("boebot_tof_capture_", ".py");
            Files.writeString(tmpScript, TOF_CAPTURE_SCRIPT);
            tmpScript.toFile().deleteOnExit();
        } catch (Exception e) {
            System.out.println("  [ERROR] Cannot write temp script: " + e.getMessage());
            logger.logFail("ArduCam ToF Capture", "temp file error: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        // ---- Run capture ----
        System.out.println();
        System.out.println("[Step 3] Capturing ToF frame...");
        System.out.println("  Command: python3 " + tmpScript + " " + logDir + " " + timestamp);
        System.out.println("  Please wait...");
        System.out.println();

        List<String> savedFiles = new ArrayList<>();
        String failReason = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "python3", tmpScript.toString(),
                logDir.getAbsolutePath(),
                timestamp
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("  [tof] " + line);
                    if (line.startsWith("SAVED:")) {
                        savedFiles.add(line.substring(6).trim());
                    } else if (line.startsWith("FAIL:")) {
                        failReason = line.substring(5).trim();
                    }
                }
            }

            int exitCode = process.waitFor();
            logger.log("  Exit code: " + exitCode);

            if (!savedFiles.isEmpty()) {
                System.out.println();
                System.out.println("TOF OUTPUT SAVED:");
                System.out.println("Folder: " + logDir.getAbsolutePath());
                System.out.println("Files:");
                for (String f : savedFiles) {
                    System.out.println("  - " + new File(f).getName());
                }
                System.out.println("Full paths:");
                for (String f : savedFiles) {
                    System.out.println("  - " + f);
                    logger.log("  Saved: " + f);
                }
                logger.logPass("ArduCam ToF Capture/Save CAM1");
                System.out.println();
                System.out.println("[RESULT] PASS - ToF frame captured and saved (" + savedFiles.size() + " file(s)).");
                return true;

            } else {
                System.out.println();
                System.out.println("  No files were saved.");
                if (failReason != null) {
                    System.out.println("  Error: " + failReason);
                }
                System.out.println();
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - Run option 10 (SDK detection) to check readiness.");
                System.out.println("  - Run: bash ~/Arducam_tof_camera/Install_dependencies.sh");
                System.out.println("  - Run: ./scripts/install_boebot.sh  (installs numpy + cv2 + SDK)");
                System.out.println("  - Check CAM1 ribbon cable.");
                String logReason = failReason != null ? failReason : "no files saved, exit " + exitCode;
                logger.logFail("ArduCam ToF Capture", logReason);
                System.out.println();
                System.out.println("[RESULT] FAIL");
                return false;
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("  [ERROR] " + msg);
            logger.logFail("ArduCam ToF Capture", msg != null ? msg : "unknown error");
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
