package my.boebot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LineSensorTest - Menu Option 17: SumoBot line sensor test (Parallax QTI).
 *
 * The QTI uses a QRD1114 IR reflective sensor read by the RC-time method:
 * charge the signal line HIGH for ~1 ms, switch it to input, then time how long
 * it stays HIGH as the capacitor discharges. A BLACK surface (playing field)
 * reflects little IR, so the discharge is slow (long RC-time); a WHITE border
 * reflects strongly, so the discharge is fast (short RC-time).
 *
 * Signal pins come from config (default: right = GPIO5 / pin 29,
 * left = GPIO6 / pin 31). The QTI W (power) pin is assumed on permanent 5V.
 *
 * Live status: "LEFT line detected", "RIGHT line detected", "BOTH lines
 * detected", or "no line (white)". Press any key to stop.
 *
 * GPIO is read from Python via lgpio (the Pi 5 native GPIO library).
 */
public class LineSensorTest {

    private static final String LINE_SCRIPT = """
            #!/usr/bin/env python3
            # BOEBOT SumoBot QTI line sensor (RC-time via lgpio)
            import sys, time

            def fail(msg):
                print("FAIL: " + str(msg), flush=True)
                sys.exit(1)

            try:
                import lgpio
            except ImportError:
                fail("python3-lgpio not installed. Run: sudo apt-get install -y python3-lgpio")

            RIGHT = int(sys.argv[1])
            LEFT  = int(sys.argv[2])
            THRESH = float(sys.argv[3])   # microseconds; above this = BLACK

            # Pi 5's 40-pin header is on gpiochip4 on some kernels, gpiochip0 on
            # others - try both.
            h = None
            for chip in (4, 0, 1):
                try:
                    h = lgpio.gpiochip_open(chip)
                    break
                except Exception:
                    h = None
            if h is None:
                fail("Cannot open any GPIO chip (tried 4, 0, 1).")

            # No internal pull resistor for the RC-time read: a pull-up would
            # hold the line high almost indefinitely (always times out ->
            # always reads as "black"); a pull-down would drain the
            # capacitor instantly (always reads as "white"). Either way the
            # QTI's own RC-time (470 ohm + capacitor + phototransistor)
            # would be swamped and the reading would stop responding to the
            # surface, which matches "detects only when unplugged, no
            # change when plugged in".
            PULL_NONE = getattr(lgpio, "SET_PULL_NONE", 0)

            def rctime(pin, timeout_us=8000.0):
                # Charge the capacitor, then time the discharge while input is HIGH.
                lgpio.gpio_claim_output(h, pin, 1)
                time.sleep(0.001)
                lgpio.gpio_free(h, pin)
                lgpio.gpio_claim_input(h, pin, PULL_NONE)
                t0 = time.perf_counter()
                limit = timeout_us / 1.0e6
                while lgpio.gpio_read(h, pin) == 1:
                    if (time.perf_counter() - t0) > limit:
                        break
                us = (time.perf_counter() - t0) * 1.0e6
                lgpio.gpio_free(h, pin)
                return us

            print("BOEBOT: Line sensor running. BLACK = RC-time > " + str(int(THRESH)) + " us.", flush=True)
            print("BOEBOT: Move sensors over black field / white border. Press any key to stop.", flush=True)

            try:
                while True:
                    r = rctime(RIGHT)
                    l = rctime(LEFT)
                    right_black = r > THRESH
                    left_black  = l > THRESH
                    if left_black and right_black:
                        status = "BOTH lines detected"
                    elif left_black:
                        status = "LEFT line detected"
                    elif right_black:
                        status = "RIGHT line detected"
                    else:
                        status = "no line (white)"
                    print("L=%6.0f us  R=%6.0f us  ->  %s" % (l, r, status), flush=True)
                    time.sleep(0.15)
            except KeyboardInterrupt:
                pass
            finally:
                try:
                    lgpio.gpiochip_close(h)
                except Exception:
                    pass
            """;

    public static boolean run(AppLogger logger, BotConfig config) {
        int rightGpio = config.getLineSensorRightGpio();
        int leftGpio  = config.getLineSensorLeftGpio();
        int threshUs  = config.getLineSensorThresholdUs();

        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test 17: Line Sensor Test (SumoBot QTI)");
        System.out.println("====================================");
        System.out.println("  Right QTI signal : GPIO" + rightGpio);
        System.out.println("  Left  QTI signal : GPIO" + leftGpio);
        System.out.println("  Black threshold  : " + threshUs + " us (RC-time)");
        System.out.println("  QTI W (power) pin assumed on permanent 5V.");

        logger.logSeparator();
        logger.log("TEST 17: Line Sensor (QTI) - right GPIO" + rightGpio
            + ", left GPIO" + leftGpio + ", threshold " + threshUs + "us");

        Path tmpScript;
        try {
            tmpScript = Files.createTempFile("boebot_line_", ".py");
            Files.writeString(tmpScript, LINE_SCRIPT);
            tmpScript.toFile().deleteOnExit();
        } catch (Exception e) {
            System.out.println("  [ERROR] Cannot write temp script: " + e.getMessage());
            logger.logFail("Line Sensor", "temp file error: " + e.getMessage());
            System.out.println("[RESULT] FAIL");
            return false;
        }

        System.out.println();
        System.out.println("[Step 1] Reading line sensors... (press any key to stop)");
        System.out.println();

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "python3", tmpScript.toString(),
                String.valueOf(rightGpio), String.valueOf(leftGpio), String.valueOf(threshUs));
            pb.redirectErrorStream(true);
            process = pb.start();

            final Process proc = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("  [line] " + line);
                    }
                } catch (Exception ignore) {}
            });
            reader.setDaemon(true);
            reader.start();

            // Detect an early failure (e.g. lgpio missing) before waiting for a key.
            Thread.sleep(600);
            try {
                int early = process.exitValue();
                System.out.println();
                System.out.println("  [WARNING] Line sensor script exited (code " + early + ").");
                System.out.println("  If lgpio is missing: sudo apt-get install -y python3-lgpio");
                logger.logFail("Line Sensor", "script exited early, code " + early);
                System.out.println("[RESULT] FAIL");
                return false;
            } catch (IllegalThreadStateException stillRunning) {
                // good
            }

            // Block until the user presses a key, then stop.
            RawKey.enableRawMode();
            RawKey.readKey();
            RawKey.restoreMode();

            process.destroy();
            process.waitFor();

            logger.logPass("Line Sensor Test (QTI)");
            System.out.println();
            System.out.println("[RESULT] PASS - Line sensor test ended.");
            System.out.println("  If black/white did not change the status, tune line.sensor.threshold.us");
            System.out.println("  in config/boebot.properties, or check the GPIO wiring.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            if (process != null) process.destroy();
            System.out.println("  [ERROR] " + e.getMessage());
            logger.logFail("Line Sensor", e.getMessage() != null ? e.getMessage() : "unknown");
            System.out.println("[RESULT] FAIL");
            return false;
        }
    }
}
