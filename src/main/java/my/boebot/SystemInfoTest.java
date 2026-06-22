package my.boebot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SystemInfoTest - Menu Option 1: Show system information.
 *
 * Displays and logs:
 *   - Hostname
 *   - OS name and version
 *   - Java version
 *   - Current date/time
 *   - Available disk space
 *   - Available memory
 */
public class SystemInfoTest {

    public static boolean run(AppLogger logger) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 1: System Information");
        System.out.println("====================================");

        logger.logSeparator();
        logger.log("TEST 1: System Information");

        try {
            // Hostname
            String hostname = "unknown";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostname = System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "unknown";
            }

            String osName    = System.getProperty("os.name", "unknown");
            String osVersion = System.getProperty("os.version", "unknown");
            String osArch    = System.getProperty("os.arch", "unknown");
            String javaVer   = System.getProperty("java.version", "unknown");
            String javaVend  = System.getProperty("java.vendor", "unknown");
            String userHome  = System.getProperty("user.home", "unknown");
            String userDir   = System.getProperty("user.dir", "unknown");
            String now       = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.println("  Hostname       : " + hostname);
            System.out.println("  OS             : " + osName + " " + osVersion);
            System.out.println("  Architecture   : " + osArch);
            System.out.println("  Java Version   : " + javaVer);
            System.out.println("  Java Vendor    : " + javaVend);
            System.out.println("  User Home      : " + userHome);
            System.out.println("  Working Dir    : " + userDir);
            System.out.println("  Date/Time      : " + now);

            logger.log("  Hostname       : " + hostname);
            logger.log("  OS             : " + osName + " " + osVersion);
            logger.log("  Architecture   : " + osArch);
            logger.log("  Java Version   : " + javaVer);
            logger.log("  Java Vendor    : " + javaVend);
            logger.log("  Date/Time      : " + now);

            // OS distribution from /etc/os-release (e.g. Bookworm vs Trixie)
            detectOsRelease(logger);

            // Disk space
            File root = new File("/");
            if (root.exists()) {
                long totalMB = root.getTotalSpace() / (1024 * 1024);
                long freeMB  = root.getFreeSpace()  / (1024 * 1024);
                long usedMB  = totalMB - freeMB;
                System.out.printf("  Disk (/)       : %d MB used / %d MB total%n", usedMB, totalMB);
                logger.log("  Disk (/)       : " + usedMB + " MB used / " + totalMB + " MB total");
            }

            // Available heap memory
            Runtime runtime = Runtime.getRuntime();
            long maxMB  = runtime.maxMemory()   / (1024 * 1024);
            long freeMB2 = runtime.freeMemory() / (1024 * 1024);
            System.out.printf("  JVM Memory     : %d MB max heap, %d MB free%n", maxMB, freeMB2);
            logger.log("  JVM Memory     : " + maxMB + " MB max heap, " + freeMB2 + " MB free");

            // Try to detect Raspberry Pi model via /proc/cpuinfo
            detectRpiModel(logger);

            logger.logPass("System Info");
            System.out.println();
            System.out.println("[RESULT] PASS - System information collected.");
            return true;

        } catch (Exception e) {
            logger.logFail("System Info", e.getMessage());
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            return false;
        }
    }

    /** Reads /etc/os-release and prints PRETTY_NAME + VERSION_CODENAME. */
    private static void detectOsRelease(AppLogger logger) {
        File osRelease = new File("/etc/os-release");
        if (!osRelease.exists()) {
            System.out.println("  OS Release     : (no /etc/os-release on this OS)");
            return;
        }
        String prettyName = null;
        String codename = null;
        try (BufferedReader reader = new BufferedReader(
                new java.io.FileReader(osRelease))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PRETTY_NAME=")) {
                    prettyName = stripQuotes(line.substring("PRETTY_NAME=".length()));
                } else if (line.startsWith("VERSION_CODENAME=")) {
                    codename = stripQuotes(line.substring("VERSION_CODENAME=".length()));
                }
            }
        } catch (Exception e) {
            // Not critical
        }
        if (prettyName == null) {
            prettyName = "(unknown)";
        }
        String out = prettyName
            + (codename != null && !codename.isEmpty() ? "  (codename: " + codename + ")" : "");
        System.out.println("  OS Release     : " + out);
        logger.log("  OS Release     : " + out);
    }

    private static String stripQuotes(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static void detectRpiModel(AppLogger logger) {
        // /proc/cpuinfo contains the RPi model on Linux
        File cpuInfo = new File("/proc/cpuinfo");
        if (!cpuInfo.exists()) {
            System.out.println("  RPi Model      : (not detectable on this OS)");
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new java.io.FileReader(cpuInfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Model")) {
                    String model = line.split(":")[1].trim();
                    System.out.println("  RPi Model      : " + model);
                    logger.log("  RPi Model      : " + model);
                    return;
                }
            }
        } catch (Exception e) {
            // Not critical
        }
    }
}
