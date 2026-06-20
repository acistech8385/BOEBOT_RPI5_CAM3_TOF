package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ToFCameraTest - Menu Option 10: ArduCam ToF camera check on CAM1.
 *
 * Steps:
 *   1. Check if ~/Arducam_tof_camera exists.
 *   2. If not found: print install instructions.
 *   3. If found: list Python demo/example files.
 *   4. Identify the best preview example.
 *   5. Print the command to run it manually.
 *   6. (Interactive mode only) Ask user if they want to run a preview now.
 *      If yes: run the example, wait for user to press Enter to stop.
 *
 * This test does NOT move wheels or gripper.
 *
 * Note: When called from the Full Hardware Test (option 11),
 * scanner is null and the interactive prompt is skipped.
 */
public class ToFCameraTest {

    private static final String ARDUCAM_FOLDER   = "Arducam_tof_camera";
    private static final String INSTALL_SCRIPT   = "Install_dependencies_raspbian.sh";

    // Python keywords that suggest a preview/depth example
    private static final String[] PREVIEW_KEYWORDS = {
        "preview", "depth", "demo", "display", "viewer", "show", "live"
    };

    /**
     * Run the ToF camera check.
     *
     * @param scanner  null = non-interactive (skip run-preview prompt).
     *                 non-null = interactive (ask user about running a preview).
     */
    public static boolean run(AppLogger logger, BotConfig config, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 10: ArduCam ToF CAM1 Check");
        System.out.println("====================================");
        System.out.println("  Camera port : CAM/DISP 1");
        System.out.println("  SDK folder  : ~/" + ARDUCAM_FOLDER);
        System.out.println();
        System.out.println("  NOTE: This test does NOT start camera automatically.");
        System.out.println("  You will be asked before any preview runs.");

        logger.logSeparator();
        logger.log("TEST 10: ArduCam ToF Camera - CAM/DISP 1");

        String homeDir = System.getProperty("user.home", "/home/pi");
        Path tofPath   = Paths.get(homeDir, ARDUCAM_FOLDER);

        // ---- Step 1: Check SDK folder ----
        System.out.println();
        System.out.println("[Step 1] Checking for SDK: " + tofPath);
        logger.log("  Checking: " + tofPath);

        if (!Files.exists(tofPath)) {
            System.out.println("  NOT FOUND: " + tofPath);
            printInstallInstructions(tofPath);
            logger.logFail("ArduCam ToF CAM1", "~/Arducam_tof_camera not found");
            System.out.println();
            System.out.println("[RESULT] FAIL - ArduCam ToF SDK not installed.");
            System.out.println("  This is expected if you have not installed the SDK yet.");
            System.out.println("  Run scripts/install_boebot.sh to install it automatically.");
            return false;
        }

        System.out.println("  FOUND: " + tofPath);
        logger.log("  FOUND: " + tofPath);

        // ---- Step 2: Check install script ----
        Path installScript = tofPath.resolve(INSTALL_SCRIPT);
        if (Files.exists(installScript)) {
            System.out.println("  Install script : " + installScript);
            System.out.println("  (If camera is not working: bash " + installScript + ")");
        } else {
            System.out.println("  Install script : Not found (SDK may be incomplete)");
        }

        // ---- Step 3: Find Python example files ----
        System.out.println();
        System.out.println("[Step 2] Scanning for Python example files...");
        logger.log("  Scanning for Python examples in " + tofPath);

        List<Path> pythonFiles = findPythonFiles(tofPath);

        if (pythonFiles.isEmpty()) {
            System.out.println("  No Python files found. SDK may be incomplete.");
            System.out.println("  Try: cd " + tofPath + " && git pull");
            logger.log("  No Python example files found.");
        } else {
            System.out.println("  Found " + pythonFiles.size() + " Python file(s):");
            for (Path f : pythonFiles) {
                String relative = homeDir + "/" + tofPath.relativize(f.getParent()) + "/" + f.getFileName();
                // Simplify path for display
                String display = f.toString().replace(homeDir, "~");
                System.out.println("    " + display);
                logger.log("    " + display);
            }
        }

        // ---- Step 4: Find best preview example ----
        Path bestPreview = findBestPreviewExample(pythonFiles);

        System.out.println();
        System.out.println("[Step 3] ArduCam ToF preview example:");
        if (bestPreview != null) {
            String displayPath = bestPreview.toString().replace(homeDir, "~");
            System.out.println("  Best candidate : " + displayPath);
            System.out.println();
            System.out.println("  To run manually (requires display):");
            System.out.println("    cd " + tofPath);
            System.out.println("    python3 " + tofPath.relativize(bestPreview));
            System.out.println();
            System.out.println("  To stop the preview: press Ctrl+C in the terminal.");
            logger.log("  Preview candidate: " + displayPath);

            // ---- Step 5: Interactive - offer to run preview ----
            if (scanner != null) {
                offerToRunPreview(logger, bestPreview, tofPath, scanner);
            } else {
                System.out.println("  (Non-interactive mode - skipping run prompt)");
            }
        } else {
            System.out.println("  No clear preview example found in the SDK.");
            System.out.println();
            if (!pythonFiles.isEmpty()) {
                System.out.println("  Try running one of the Python files listed above manually:");
                System.out.println("    cd " + tofPath);
                System.out.println("    python3 <filename>.py");
            } else {
                System.out.println("  Install or update the SDK:");
                System.out.println("    cd " + tofPath + " && git pull");
            }
        }

        // ---- Check Python3 is available ----
        System.out.println();
        System.out.println("[Step 4] Checking Python3...");
        boolean python3Ok = checkPython3();
        if (python3Ok) {
            System.out.println("  Python 3 is available.");
        } else {
            System.out.println("  Python 3 not found. Install: sudo apt-get install -y python3");
            logger.log("  WARNING: python3 not found.");
        }

        logger.logPass("ArduCam ToF CAM1 SDK Detection");
        System.out.println();
        System.out.println("[RESULT] PASS - ArduCam ToF SDK found.");
        System.out.println("  Camera preview was not started automatically.");
        if (bestPreview != null) {
            System.out.println("  Run the preview manually from the command above.");
        }
        return true;
    }

    /** Scans for Python files inside the ToF SDK folder. */
    private static List<Path> findPythonFiles(Path root) {
        List<Path> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "find", root.toString(),
                "-name", "*.py",
                "-not", "-path", "*/__pycache__/*",
                "-not", "-path", "*/.git/*"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        result.add(Paths.get(line));
                        if (result.size() >= 30) break; // cap to avoid flooding output
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // find command not available or other issue
        }
        return result;
    }

    /**
     * From a list of Python files, finds the one most likely to be a live preview.
     * Prefers files whose name contains "preview", "depth", "demo", etc.
     */
    private static Path findBestPreviewExample(List<Path> files) {
        // First pass: look for files with preview-related keywords in the name
        for (String keyword : PREVIEW_KEYWORDS) {
            for (Path f : files) {
                String name = f.getFileName().toString().toLowerCase();
                if (name.contains(keyword)) {
                    return f;
                }
            }
        }
        // Second pass: return the first Python file if any exist
        if (!files.isEmpty()) {
            return files.get(0);
        }
        return null;
    }

    /** Offers the user a chance to run a Python preview example interactively. */
    private static void offerToRunPreview(AppLogger logger, Path script,
                                          Path sdkRoot, Scanner scanner) {
        // Check display
        String displayEnv  = System.getenv("DISPLAY");
        String waylandEnv  = System.getenv("WAYLAND_DISPLAY");
        boolean hasDisplay = (displayEnv != null && !displayEnv.isEmpty())
                          || (waylandEnv  != null && !waylandEnv.isEmpty());

        if (!hasDisplay) {
            System.out.println("  No display detected (DISPLAY / WAYLAND_DISPLAY not set).");
            System.out.println("  Live preview requires a display. Skipping run prompt.");
            return;
        }

        if (!checkPython3()) {
            System.out.println("  Python 3 not found. Cannot run example.");
            return;
        }

        System.out.println("  Do you want to run this preview example now?");
        System.out.println("  Press Enter to STOP the preview when done.");
        System.out.print("  Run preview? (y/n): ");

        String answer = scanner.nextLine().trim().toLowerCase();
        if (!answer.equals("y") && !answer.equals("yes")) {
            System.out.println("  Skipped.");
            return;
        }

        System.out.println();
        System.out.println("  Starting: python3 " + script);
        System.out.println("  Press ENTER in this terminal to stop.");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", script.toString());
            pb.directory(sdkRoot.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output in background thread
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.isBlank()) {
                            System.out.println("  [tof] " + line);
                        }
                    }
                } catch (Exception ignore) {}
            });
            outputThread.setDaemon(true);
            outputThread.start();

            // Check for early exit (error)
            Thread.sleep(800);
            try {
                int earlyExit = process.exitValue();
                System.out.println("  [WARNING] Preview script exited early (exit code " + earlyExit + ").");
                System.out.println("  Check that the ArduCam ToF camera is connected to CAM1.");
                logger.log("  ToF preview exited early. Exit: " + earlyExit);
                return;
            } catch (IllegalThreadStateException e) {
                // Good - still running
                System.out.println("  Preview is running.");
            }

            // Wait for user
            System.out.print("  Press ENTER to stop: ");
            scanner.nextLine();

            process.destroy();
            try { process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignore) {}
            System.out.println("  Preview stopped.");
            logger.log("  ToF preview stopped by user.");

        } catch (Exception e) {
            System.out.println("  [ERROR] Could not run example: " + e.getMessage());
            logger.log("  ToF preview error: " + e.getMessage());
        }
    }

    private static boolean checkPython3() {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "python3");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void printInstallInstructions(Path tofPath) {
        System.out.println();
        System.out.println("  ArduCam ToF SDK not found.");
        System.out.println();
        System.out.println("  Option 1 - Run the BOEBOT install script (recommended):");
        System.out.println("    ./scripts/install_boebot.sh");
        System.out.println("  This will clone the SDK automatically.");
        System.out.println();
        System.out.println("  Option 2 - Manual install:");
        System.out.println("    git clone https://github.com/ArduCAM/Arducam_tof_camera.git "
            + tofPath);
        System.out.println("    bash " + tofPath + "/Install_dependencies_raspbian.sh");
    }
}
