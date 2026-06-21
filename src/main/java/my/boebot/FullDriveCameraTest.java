package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * FullDriveCameraTest - Menu Option 16: drive the robot while watching both
 * cameras live.
 *
 * Launches the dual camera live view (Camera Module 3 RGB + ArduCam ToF depth)
 * in a window, then runs the same drive control as option 7 in the terminal:
 *   8 = forward, 2 = backward, 4 = turn left, 6 = turn right, 5 = stop,
 *   ESC / q = exit.
 *
 * Drive from the terminal while watching the camera window. The camera needs a
 * display (HDMI/VNC/X11); with no display the camera is skipped and you can
 * still drive. Ctrl+C is safe (all servo outputs are cut).
 */
public class FullDriveCameraTest {

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        System.out.println();
        System.out.println("============================================");
        System.out.println("  Test 16: Full Test - Drive + Dual Live Camera");
        System.out.println("============================================");
        System.out.println("  Left  view : Camera Module 3 RGB  (CAM0)");
        System.out.println("  Right view : ArduCam ToF depth     (CAM1)");
        System.out.println("  Drive: 8 fwd  2 back  4 left  6 right  5 stop  ESC/q exit");

        logger.logSeparator();
        logger.log("TEST 16: Full Test - Drive + Dual Live Camera");

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail("Full Drive+Camera", "Pi4J not available");
            return false;
        }

        // ---- Start the dual camera view if a display is available ----
        String displayEnv = System.getenv("DISPLAY");
        String waylandEnv = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (displayEnv != null && !displayEnv.isEmpty())
                          || (waylandEnv != null && !waylandEnv.isEmpty());

        Process camera = null;
        System.out.println();
        if (hasDisplay) {
            System.out.println("[Step 1] Starting dual camera live view...");
            camera = DualCameraViewTest.launchBackground();
            if (camera != null) {
                System.out.println("  Dual camera window opening (RGB | depth).");
            }
        } else {
            System.out.println("[Step 1] No display detected - camera view skipped.");
            System.out.println("  (Use VNC or an HDMI screen to see the cameras while driving.)");
            logger.log("  Full test: no display, camera view skipped.");
        }

        // ---- Drive control ----
        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());
            DriveControlTest.stopBoth(pca);

            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("[Step 2] Drive now. 8/2/4/6 to move, 5 = stop, ESC/q = exit.");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            RawKey.enableRawMode();
            DriveControlTest.driveLoop(pca, logger);
            RawKey.restoreMode();

            logger.logPass("Full Test - Drive + Dual Camera");
            System.out.println();
            System.out.println("[RESULT] PASS - Full drive + camera test ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail("Full Drive+Camera", e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { DriveControlTest.stopBoth(pca); } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
            if (camera != null) {
                try { camera.destroy(); } catch (Exception ignore) {}
                System.out.println("  Dual camera view closed.");
            }
        }
    }
}
