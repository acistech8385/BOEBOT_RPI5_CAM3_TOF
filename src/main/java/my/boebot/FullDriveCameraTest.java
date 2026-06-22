package my.boebot;

import com.pi4j.context.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * FullDriveCameraTest - Menu Option 17: drive the robot while watching both
 * cameras, all in ONE window.
 *
 * Unlike option 8 (which drives from the terminal), here the driving keys are
 * read by the camera window itself, so there is no focus tug-of-war between the
 * terminal and the OpenCV window. Press the keys while the camera window is
 * focused:
 *   8 = forward, 2 = backward, 4 = turn left, 6 = turn right, 5 = stop,
 *   Q / ESC = quit.
 *
 * The Java side initialises the PCA9685 to 50 Hz (the setting persists in the
 * chip), then launches a Python window that shows both cameras and sets the
 * wheel pulses via i2cset. On exit all servo outputs are cut.
 *
 * Needs a display (HDMI/VNC/X11). Calibrate the wheels first (option 4) or an
 * untrimmed wheel will creep even when stopped.
 */
public class FullDriveCameraTest {

    private static final String FULL_SCRIPT = """
            #!/usr/bin/env python3
            # BOEBOT Full Test: drive (in this window) + dual live camera
            import sys, time, subprocess

            def log(m): print("BOEBOT: " + str(m), flush=True)
            def fail(m): print("FAIL: " + str(m), flush=True); sys.exit(1)

            try:
                import numpy as np
            except ImportError:
                fail("numpy not found")
            try:
                import cv2
            except ImportError:
                fail("cv2 not found")
            try:
                from picamera2 import Picamera2
            except ImportError as e:
                fail("picamera2 not importable: " + str(e))
            try:
                import ArducamDepthCamera as ac
            except ImportError as e:
                fail("ArducamDepthCamera not importable: " + str(e))

            # -- Servo control via i2cset. The app already set the PCA9685 to 50 Hz. --
            RIGHT_CH, LEFT_CH = 14, 15
            def _set(reg, val):
                subprocess.run(["i2cset", "-y", "1", "0x40", str(reg), str(val)],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            def set_pulse(ch, us):
                off = int(round(us / 20000.0 * 4096))
                base = 0x06 + ch * 4
                _set(base + 0, 0)
                _set(base + 1, 0)
                _set(base + 2, off & 0xFF)
                _set(base + 3, (off >> 8) & 0x0F)
            def drive(rp, lp):
                set_pulse(RIGHT_CH, rp)
                set_pulse(LEFT_CH, lp)
            def stop():
                drive(1500, 1500)

            VIEW_W, VIEW_H = 640, 480
            CAP_W, CAP_H   = 1280, 720

            log("Opening Camera Module 3 (RGB, CAM0)...")
            try:
                picam2 = Picamera2(0)
                cfg = picam2.create_video_configuration(
                    main={"size": (CAP_W, CAP_H), "format": "RGB888"})
                picam2.configure(cfg)
                picam2.start()
                try:
                    picam2.set_controls({"AfMode": 2, "AfTrigger": 0})
                except Exception:
                    pass
            except Exception as e:
                fail("Camera Module 3 open failed: " + str(e))

            log("Opening ArduCam ToF (depth, CAM1)...")
            cam = ac.ArducamCamera()
            opened = None
            for idx in [8, 0, 9, 10, 11, 12]:
                if cam.open(ac.Connection.CSI, idx) == 0:
                    opened = idx
                    log("ToF opened on /dev/video" + str(idx))
                    break
            if opened is None:
                picam2.stop()
                fail("ToF open failed")
            if cam.start(ac.FrameType.DEPTH) != 0:
                cam.close()
                picam2.stop()
                fail("ToF start failed")
            try:
                cam.setControl(ac.Control.RANGE, 4000)
            except Exception:
                pass
            r = 4000

            stop()
            WINDOW = "BOEBOT Full -- drive HERE | RGB (CAM0) | Depth (CAM1)"
            cv2.namedWindow(WINDOW, cv2.WINDOW_NORMAL)
            log("Drive in THIS window: 8 fwd, 2 back, 4 left, 6 right, 5 stop, Q/ESC quit.")
            depth_vis = np.zeros((VIEW_H, VIEW_W, 3), dtype="uint8")

            try:
                while True:
                    rgb = picam2.capture_array()
                    rgb = cv2.resize(rgb, (VIEW_W, VIEW_H), interpolation=cv2.INTER_AREA)

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
                    cv2.putText(left, "RGB CAM0", (8, 22),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 255, 0), 2)
                    cv2.putText(right, "Depth CAM1", (8, 22),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 255, 255), 2)
                    cv2.putText(left, "8 fwd 2 back 4 left 6 right 5 stop Q quit",
                                (8, VIEW_H - 12), cv2.FONT_HERSHEY_SIMPLEX, 0.45,
                                (255, 255, 255), 1)

                    combo = cv2.hconcat([left, right])
                    cv2.imshow(WINDOW, combo)

                    k = cv2.waitKey(30) & 0xFF
                    if k == ord("8"):
                        drive(1450, 1550); log("FORWARD")
                    elif k == ord("2"):
                        drive(1550, 1450); log("BACKWARD")
                    elif k == ord("6"):
                        drive(1550, 1550); log("TURN RIGHT")
                    elif k == ord("4"):
                        drive(1450, 1450); log("TURN LEFT")
                    elif k == ord("5"):
                        stop(); log("STOP")
                    elif k in (ord("q"), ord("Q"), 27):
                        break
                    try:
                        if cv2.getWindowProperty(WINDOW, cv2.WND_PROP_VISIBLE) < 1:
                            break
                    except cv2.error:
                        pass
            finally:
                stop()
                cam.stop()
                cam.close()
                picam2.stop()
                cv2.destroyAllWindows()
                log("Full test ended. Servos stopped.")
            """;

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("============================================");
        System.out.println("  Test 17: Full Test - Drive + Dual Live Camera");
        System.out.println("============================================");
        System.out.println("  Drive IN the camera window: 8 fwd 2 back 4 left 6 right 5 stop Q quit");
        System.out.println("  Calibrate wheels first (option 4) so they fully stop.");

        logger.logSeparator();
        logger.log("TEST 17: Full Test - Drive + Dual Live Camera");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Full Drive+Camera", "Pi4J not available");
            return false;
        }

        // ---- Display required (driving happens in the camera window) ----
        String displayEnv = System.getenv("DISPLAY");
        String waylandEnv = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (displayEnv != null && !displayEnv.isEmpty())
                          || (waylandEnv != null && !waylandEnv.isEmpty());
        if (!hasDisplay) {
            System.out.println();
            System.out.println("  FULL TEST NEEDS A DISPLAY (you drive inside the camera window).");
            System.out.println("  Use VNC or an HDMI screen. For headless driving use option 8.");
            logger.logFail("Full Drive+Camera", "No display detected");
            System.out.println("[RESULT] FAIL - No display.");
            return false;
        }

        int bus  = config.getI2cBus();
        int addr = config.getI2cAddress();

        // ---- Initialise the PCA9685 to 50 Hz (persists in the chip) ----
        System.out.println();
        System.out.println("[Step 1] Initialising PCA9685 (50 Hz) and stopping wheels...");
        try {
            PCA9685 pca = new PCA9685(pi4j, bus, addr);
            pca.initialize(config.getPwmFrequency());
            pca.setServoPulse(14, 1500);
            pca.setServoPulse(15, 1500);
            pca.close();
        } catch (Exception e) {
            System.out.println("  [ERROR] PCA9685 init failed: " + e.getMessage());
            logger.logFail("Full Drive+Camera", "PCA9685 init: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        // ---- Write + launch the combined window ----
        Path tmpScript;
        try {
            tmpScript = Files.createTempFile("boebot_full_", ".py");
            Files.writeString(tmpScript, FULL_SCRIPT);
            tmpScript.toFile().deleteOnExit();
        } catch (Exception e) {
            System.out.println("  [ERROR] Cannot write temp script: " + e.getMessage());
            logger.logFail("Full Drive+Camera", "temp file error: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        Thread stopHook = ServoSafety.installStopHook(bus, addr);
        System.out.println();
        System.out.println("[Step 2] Opening the drive + camera window...");
        System.out.println("  >>> Click the window, then drive with 8/2/4/6, 5 = stop, Q/ESC = quit.");
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
                        if (!line.isBlank()) System.out.println("  [full] " + line);
                    }
                } catch (Exception ignore) {}
            });
            reader.setDaemon(true);
            reader.start();

            int exitCode = process.waitFor();
            // Always cut servo outputs after the window closes.
            ServoSafety.allOutputsOff(bus, addr);

            System.out.println();
            if (exitCode == 0) {
                logger.logPass("Full Test - Drive + Dual Camera");
                System.out.println("[RESULT] PASS - Full drive + camera test ended.");
                return true;
            } else {
                System.out.println("  Window exited with code " + exitCode + " - check [full] lines above.");
                System.out.println("  POSSIBLE FIXES:");
                System.out.println("  - sudo apt-get install -y python3-picamera2 python3-opencv");
                System.out.println("  - bash ~/Arducam_tof_camera/Install_dependencies.sh");
                logger.logFail("Full Drive+Camera", "python exit " + exitCode);
                System.out.println("[RESULT] FAIL");
                return false;
            }

        } catch (Exception e) {
            ServoSafety.allOutputsOff(bus, addr);
            System.out.println("  [ERROR] " + e.getMessage());
            logger.logFail("Full Drive+Camera", e.getMessage() != null ? e.getMessage() : "unknown");
            System.out.println("[RESULT] FAIL");
            return false;
        } finally {
            ServoSafety.removeStopHook(stopHook);
        }
    }
}
