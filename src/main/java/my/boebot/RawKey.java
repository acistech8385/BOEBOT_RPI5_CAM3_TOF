package my.boebot;

import java.io.IOException;

/**
 * RawKey - single-keypress reader for interactive servo control.
 *
 * Puts the Linux terminal into raw mode (via stty) so key presses are read
 * immediately without waiting for Enter, then restores normal (cooked) mode.
 *
 * Runs on the Raspberry Pi terminal. If stty is unavailable (e.g. not a TTY),
 * raw mode silently fails and reads fall back to line-buffered input.
 *
 * Key codes of interest:
 *   '8' = 56, '2' = 50, '5' = 53, ESC = 27
 */
public final class RawKey {

    private RawKey() {}

    /** Switch the terminal to raw mode: keys arrive one at a time, no echo. */
    public static void enableRawMode() {
        // min 1 time 0 = return as soon as one byte is available, no Enter needed.
        runStty("stty -echo -icanon min 1 time 0 < /dev/tty");
    }

    /** Restore normal terminal mode (line buffered, echo on). */
    public static void restoreMode() {
        runStty("stty sane < /dev/tty");
    }

    /**
     * Read a single key. Returns the byte value, or -1 on EOF/error.
     * ESC is 27; the digit keys are their ASCII codes ('8' = 56, etc.).
     */
    public static int readKey() {
        try {
            return System.in.read();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Reads and discards any bytes already buffered (non-blocking).
     *
     * Special keys like arrow keys send a multi-byte escape sequence
     * (e.g. Down = ESC '[' 'B'). readKey() only consumes the first byte, so
     * the rest would otherwise leak into the next line-based read (e.g. a
     * Scanner menu prompt) and corrupt it - "9" typed after a stray arrow
     * key becomes "[B9". Call this while still in raw mode, right after
     * readKey() and before restoreMode(), to flush any such leftovers.
     */
    public static void drainPending() {
        try {
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException ignore) {}
    }

    private static void runStty(String cmd) {
        try {
            new ProcessBuilder("sh", "-c", cmd).inheritIO().start().waitFor();
        } catch (Exception ignore) {
            // Not a TTY or stty missing - fall back to line-buffered input.
        }
    }
}
