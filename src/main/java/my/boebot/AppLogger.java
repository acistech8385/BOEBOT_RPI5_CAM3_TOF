package my.boebot;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AppLogger - Saves test results to a log file.
 *
 * Log files are saved in:
 *   logs/<hostname>/boebot_<timestamp>.log
 *
 * Each test prints PASS or FAIL to both the console and the log file.
 */
public class AppLogger {

    private static final DateTimeFormatter FILE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter LOG_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PrintWriter writer;
    private String hostname;
    private String logFilePath;

    public AppLogger() {
        hostname = detectHostname();
        String timestamp = LocalDateTime.now().format(FILE_FORMAT);

        // Create the logs/<hostname>/ directory
        String logDir = "logs" + File.separator + hostname;
        File dir = new File(logDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        logFilePath = logDir + File.separator + "boebot_" + timestamp + ".log";

        try {
            writer = new PrintWriter(new FileWriter(logFilePath, false));
            System.out.println("[Logger] Log file: " + logFilePath);
        } catch (Exception e) {
            System.out.println("[Logger] WARNING: Could not create log file: " + e.getMessage());
        }
    }

    private String detectHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** Writes a line to both console and log file. */
    public void log(String message) {
        String line = "[" + LocalDateTime.now().format(LOG_FORMAT) + "] " + message;
        System.out.println(line);
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }

    /** Logs a PASS result for a test. */
    public void logPass(String testName) {
        log("PASS - " + testName);
    }

    /** Logs a FAIL result for a test. */
    public void logFail(String testName, String reason) {
        log("FAIL - " + testName + " | Reason: " + reason);
    }

    /** Logs a separator line for readability. */
    public void logSeparator() {
        String line = "--------------------------------------------";
        System.out.println(line);
        if (writer != null) {
            writer.println(line);
            writer.flush();
        }
    }

    /** Logs system information at startup. */
    public void logSystemInfo() {
        logSeparator();
        log("BOEBOT RPi5 Hardware Test App - Session Start");
        logSeparator();
        log("Hostname     : " + hostname);
        log("OS           : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        log("OS Arch      : " + System.getProperty("os.arch"));
        log("Java Version : " + System.getProperty("java.version"));
        log("Java Vendor  : " + System.getProperty("java.vendor"));
        log("User Home    : " + System.getProperty("user.home"));
        logSeparator();
    }

    public String getHostname() {
        return hostname;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    /** Closes the log file writer. */
    public void close() {
        if (writer != null) {
            log("Session End - Log file saved: " + logFilePath);
            writer.close();
        }
    }
}
