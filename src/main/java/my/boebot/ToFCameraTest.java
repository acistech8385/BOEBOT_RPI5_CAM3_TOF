package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ToFCameraTest - Menu Option 10: ArduCam ToF SDK detection on CAM1.
 *
 * Steps:
 *   1. Check if ~/Arducam_tof_camera SDK folder exists.
 *   2. List Python example files found in the SDK.
 *   3. Check Python 3 is available.
 *   4. Check ArducamDepthCamera Python module is importable.
 *
 * PASS = SDK exists, Python3 found, and ArducamDepthCamera importable.
 * FAIL = any of the above missing.
 *
 * Use option 11 for live preview and option 12 for capture/save.
 */
public class ToFCameraTest {

    private static final String ARDUCAM_FOLDER = "Arducam_tof_camera";

    public static boolean run(AppLogger logger, BotConfig config) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 10: ArduCam ToF SDK Detection CAM1");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 1");
        System.out.println("  Type        : SDK detection and readiness check");

        logger.logSeparator();
        logger.log("TEST 10: ArduCam ToF SDK Detection - CAM/DISP 1");

        // ---- Step 1: SDK folder ----
        System.out.println();
        System.out.println("[Step 1] Checking ArduCam ToF SDK installation...");

        String homeDir = System.getProperty("user.home", "~");
        Path tofPath = Paths.get(homeDir, ARDUCAM_FOLDER);
        boolean sdkExists = Files.isDirectory(tofPath);

        if (!sdkExists) {
            System.out.println("  SDK NOT FOUND: " + tofPath);
            System.out.println();
            System.out.println("  To install:");
            System.out.println("    ./scripts/install_boebot.sh");
            System.out.println("  Or manually:");
            System.out.println("    git clone https://github.com/ArduCAM/Arducam_tof_camera.git ~/Arducam_tof_camera");
            System.out.println("    bash ~/Arducam_tof_camera/Install_dependencies_raspbian.sh");
            logger.logFail("ArduCam ToF SDK", "SDK not found at " + tofPath);
            System.out.println();
            System.out.println("[RESULT] FAIL - ArduCam ToF SDK not installed.");
            return false;
        }

        System.out.println("  SDK found: " + tofPath);
        logger.log("  SDK path: " + tofPath);

        // ---- Step 2: List Python files ----
        System.out.println();
        System.out.println("[Step 2] Listing Python example files in SDK...");
        List<String> pyFiles = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "find", tofPath.toString(), "-name", "*.py", "-not", "-path", "*/.*");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) pyFiles.add(line);
                }
            }
            p.waitFor();
        } catch (Exception e) {
            System.out.println("  [WARN] Could not list files: " + e.getMessage());
        }

        if (pyFiles.isEmpty()) {
            System.out.println("  No Python files found in " + tofPath);
        } else {
            System.out.println("  Found " + pyFiles.size() + " Python file(s):");
            for (String f : pyFiles) {
                System.out.println("    " + f.replace(homeDir, "~"));
            }
        }

        // ---- Step 3: Check Python3 ----
        System.out.println();
        System.out.println("[Step 3] Checking Python 3...");
        boolean python3ok = false;
        String python3ver = "";
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                python3ver = br.readLine();
            }
            python3ok = (p.waitFor() == 0) && python3ver != null && !python3ver.isEmpty();
        } catch (Exception ignore) {}

        if (python3ok) {
            System.out.println("  Python 3: " + python3ver);
        } else {
            System.out.println("  Python 3: NOT FOUND");
            System.out.println("  Fix: sudo apt-get install -y python3");
        }

        // ---- Step 4: Check ArducamDepthCamera module ----
        System.out.println();
        System.out.println("[Step 4] Checking ArducamDepthCamera Python module...");
        boolean sdkImportable = false;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "python3", "-c", "import ArducamDepthCamera; print('OK')");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                line = br.readLine();
            }
            sdkImportable = (p.waitFor() == 0) && "OK".equals(line);
        } catch (Exception ignore) {}

        if (sdkImportable) {
            System.out.println("  ArducamDepthCamera: importable (SDK is ready)");
            logger.log("  ArducamDepthCamera: importable");
        } else {
            System.out.println("  ArducamDepthCamera: NOT importable");
            System.out.println("  Fix: bash ~/Arducam_tof_camera/Install_dependencies_raspbian.sh");
            System.out.println("       OR:  ./scripts/install_boebot.sh");
        }

        // ---- Result ----
        System.out.println();
        boolean pass = sdkExists && python3ok && sdkImportable;
        if (pass) {
            System.out.println("  SDK: OK  |  Python3: OK  |  ArducamDepthCamera: OK");
            System.out.println("  Use option 11 (ToF live preview) or option 12 (ToF capture/save).");
            logger.logPass("ArduCam ToF SDK Detection CAM1");
            System.out.println();
            System.out.println("[RESULT] PASS - ArduCam ToF SDK is installed and ready.");
        } else {
            String reason = !python3ok ? "Python3 not found" :
                            !sdkImportable ? "ArducamDepthCamera module not importable" : "SDK check failed";
            logger.logFail("ArduCam ToF SDK Detection", reason);
            System.out.println();
            System.out.println("[RESULT] FAIL - " + reason);
        }
        return pass;
    }
}
