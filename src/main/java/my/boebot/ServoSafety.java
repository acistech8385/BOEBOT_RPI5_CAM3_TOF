package my.boebot;

/**
 * ServoSafety - emergency servo stop that survives Ctrl+C (SIGINT).
 *
 * A plain Ctrl+C kills the JVM without running finally blocks, so any servo
 * left at a non-neutral pulse keeps moving. These helpers install a JVM
 * shutdown hook that turns OFF all PCA9685 PWM outputs (so every servo stops)
 * and restores the terminal, then let a clean exit remove the hook.
 *
 * The shutdown action uses i2c-tools (i2cset) directly rather than Pi4J,
 * because the Pi4J context may already be torn down during JVM shutdown.
 */
public final class ServoSafety {

    private ServoSafety() {}

    /**
     * Turn OFF all PCA9685 PWM outputs immediately.
     * Writes 0x10 to ALL_LED_OFF_H (register 0xFD), which forces every channel
     * fully off -> no pulse -> continuous-rotation servos stop.
     */
    public static void allOutputsOff(int bus, int addr) {
        String cmd = String.format("i2cset -y %d 0x%02x 0xFD 0x10", bus, addr);
        try {
            new ProcessBuilder("sh", "-c", cmd).start().waitFor();
        } catch (Exception ignore) {
            // best effort - try with sudo as a fallback
            try {
                new ProcessBuilder("sh", "-c", "sudo " + cmd).start().waitFor();
            } catch (Exception ignore2) {}
        }
    }

    /**
     * Install a shutdown hook that stops all servos and restores the terminal.
     * Returns the hook so a clean exit can remove it via removeStopHook().
     */
    public static Thread installStopHook(int bus, int addr) {
        Thread hook = new Thread(() -> {
            allOutputsOff(bus, addr);
            RawKey.restoreMode();
        }, "servo-stop-hook");
        try {
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Exception ignore) {}
        return hook;
    }

    /** Remove a previously installed stop hook (called on a clean exit). */
    public static void removeStopHook(Thread hook) {
        if (hook == null) return;
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (Exception ignore) {
            // Hook may already be running (shutdown in progress) - ignore.
        }
    }
}
