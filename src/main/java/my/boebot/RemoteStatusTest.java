package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * RemoteStatusTest - Menu Option 21: show IP address and remote status.
 *
 * Prints the Pi's hostname, IP address, and the SSH / XRDP service status,
 * plus the Remote Desktop connection instructions.
 *
 * This test does NOT store any IP address, username, or password. The IP is
 * read live from `hostname -I` each time and only printed.
 */
public class RemoteStatusTest {

    public static boolean run(AppLogger logger) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 21: IP Address and Remote Status");
        System.out.println("====================================");

        logger.logSeparator();
        logger.log("TEST 21: IP Address and Remote Status");

        String hostname = runCmd("hostname");
        String ip       = runCmd("hostname", "-I");
        String ssh      = runCmd("systemctl", "is-active", "ssh");
        String xrdp     = runCmd("systemctl", "is-active", "xrdp");

        System.out.println();
        System.out.println("  Hostname    : " + hostname);
        System.out.println("  IP address  : " + ip);
        System.out.println("  SSH status  : " + ssh);
        System.out.println("  XRDP status : " + xrdp);
        System.out.println();
        System.out.println("  --- Connect with the Windows Remote Desktop app ---");
        System.out.println("  Computer : " + ip);
        System.out.println("  Session  : Xorg");
        System.out.println("  Username : your Raspberry Pi username");
        System.out.println("  Password : your Raspberry Pi password");
        System.out.println();

        logger.log("  Hostname: " + hostname + " | IP: " + ip
            + " | SSH: " + ssh + " | XRDP: " + xrdp);
        logger.logPass("Remote Status");
        System.out.println("[RESULT] PASS - Remote status shown.");
        return true;
    }

    /** Runs a command and returns its trimmed stdout (one line), or a marker. */
    private static String runCmd(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(line.trim());
                }
            }
            p.waitFor();
            String out = sb.toString().trim();
            return out.isEmpty() ? "(unknown)" : out;
        } catch (Exception e) {
            return "(error: " + e.getMessage() + ")";
        }
    }
}
