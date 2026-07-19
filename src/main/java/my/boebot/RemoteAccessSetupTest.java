package my.boebot;

import java.io.File;
import java.util.Scanner;

/**
 * RemoteAccessSetupTest - Menu Option 18: setup/repair Remote Access (SSH + XRDP).
 *
 * Runs scripts/setup_remote_access.sh (via ProcessBuilder, inheriting the
 * terminal so sudo prompts work). The user must type exactly SETUP_REMOTE to
 * proceed.
 *
 * This test does NOT ask for, store, or hardcode any IP address, username, or
 * password. The script only installs/enables services and prints the Pi's own
 * hostname/IP.
 */
public class RemoteAccessSetupTest {

    private static final String CONFIRM = "SETUP_REMOTE";
    private static final String SCRIPT  = "scripts/setup_remote_access.sh";

    public static boolean run(AppLogger logger, Scanner scanner) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 18: Setup/Repair Remote Access (SSH + XRDP)");
        System.out.println("====================================");
        System.out.println();
        System.out.println("  This will install/repair SSH, XRDP and Xorg packages.");
        System.out.println("  It may require sudo password and may need reboot.");
        System.out.println("  It also patches the XRDP black-screen issue, so the same");
        System.out.println("  user can be logged in on HDMI and RDP (Xorg) at once.");
        System.out.println("  On Trixie (or newer) it also disables desktop autologin");
        System.out.println("  (boot to console + login) to avoid an XRDP session clash.");
        System.out.println();
        System.out.println("  No IP address, username, or password is asked for or stored.");
        System.out.println();
        System.out.println("  To continue, type exactly:  " + CONFIRM);
        System.out.println("  Anything else cancels.");
        System.out.print("  > ");

        logger.logSeparator();
        logger.log("TEST 18: Setup/Repair Remote Access (SSH + XRDP)");

        String input = scanner.nextLine().trim();
        if (!input.equals(CONFIRM)) {
            System.out.println();
            System.out.println("  Cancelled - you did not type " + CONFIRM + ".");
            logger.log("  Remote setup cancelled by user.");
            System.out.println("[RESULT] Cancelled.");
            return false;
        }

        File script = new File(SCRIPT).getAbsoluteFile();
        if (!script.exists()) {
            System.out.println();
            System.out.println("  [ERROR] Script not found: " + script.getPath());
            System.out.println("  Run the app from the project root (use ./scripts/run_boebot_test.sh).");
            logger.logFail("Remote Access Setup", "script not found: " + script.getPath());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println();
        System.out.println("[Step 1] Running " + SCRIPT + " ...");
        System.out.println("  (Enter your sudo password if prompted.)");
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", script.getPath());
            pb.inheritIO();   // share the terminal so sudo prompts work
            Process process = pb.start();
            int exit = process.waitFor();

            System.out.println();
            if (exit == 0) {
                logger.logPass("Remote Access Setup (SSH + XRDP)");
                System.out.println("[RESULT] PASS - Remote access setup finished.");
                System.out.println("  Reboot is recommended:  sudo reboot");
                return true;
            } else {
                logger.logFail("Remote Access Setup", "script exit code " + exit);
                System.out.println("[RESULT] FAIL - Setup script exited with code " + exit + ".");
                return false;
            }

        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("  [ERROR] " + msg);
            logger.logFail("Remote Access Setup", msg != null ? msg : "unknown error");
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
