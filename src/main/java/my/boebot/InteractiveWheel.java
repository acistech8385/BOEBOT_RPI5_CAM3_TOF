package my.boebot;

import com.pi4j.context.Context;

/**
 * InteractiveWheel - shared incremental speed control for one wheel servo.
 *
 * Key controls (no Enter needed):
 *   8   = nudge faster FORWARD  (press again to speed up)
 *   2   = nudge faster REVERSE  (press again to speed up)
 *   5   = stop
 *   ESC / q = stop and exit
 *
 * Speed is a signed level from -MAX_LEVEL..+MAX_LEVEL. The pulse is
 *   1500 + fwdSign * level * STEP
 * where fwdSign is -1 for the right wheel (forward = lower pulse) and +1 for
 * the left wheel (forward = higher pulse, mirrored mounting).
 *
 * Requires the wheel servo to be trimmed to a true stop at 1500 us (option 4).
 */
public final class InteractiveWheel {

    private InteractiveWheel() {}

    private static final int STOP      = 1500;
    private static final int STEP      = 40;   // us per speed level
    private static final int MAX_LEVEL = 5;    // max 200 us from neutral (~full speed)

    public static boolean run(AppLogger logger, BotConfig config, Context pi4j,
                               int channel, int fwdSign, String name, int testNo) {
        System.out.println();
        System.out.println("====================================");
        System.out.println("  Test " + testNo + ": " + name + " (incremental)");
        System.out.println("====================================");
        System.out.println("  Servo HAT channel : " + channel);
        System.out.println("  Servo type        : Parallax continuous rotation");
        System.out.println();
        System.out.println("  Controls:  8 = faster forward   2 = faster reverse");
        System.out.println("             5 = stop             ESC / q = exit");
        System.out.println("  Reminder: lift the wheels off the ground before driving.");

        logger.logSeparator();
        logger.log("TEST " + testNo + ": " + name + " (incremental) - channel " + channel);

        if (pi4j == null) {
            System.out.println("[RESULT] FAIL - Pi4J context not available.");
            logger.logFail(name, "Pi4J not available");
            return false;
        }

        PCA9685 pca = null;
        Thread stopHook = null;
        try {
            pca = new PCA9685(pi4j, config.getI2cBus(), config.getI2cAddress());
            pca.initialize(config.getPwmFrequency());
            pca.setServoPulse(channel, STOP);

            // Safety: stop the servo even on Ctrl+C.
            stopHook = ServoSafety.installStopHook(config.getI2cBus(), config.getI2cAddress());

            System.out.println();
            System.out.println("  Ready. Press 8 / 2 / 5, or ESC (or q) to exit...");
            System.out.println("  (If a single key does nothing, press Enter after it.)");
            System.out.println();

            int level = 0;
            RawKey.enableRawMode();
            boolean exit = false;
            while (!exit) {
                int key = RawKey.readKey();
                switch (key) {
                    case '8' -> {
                        if (level < MAX_LEVEL) level++;
                        apply(pca, channel, fwdSign, level, logger);
                    }
                    case '2' -> {
                        if (level > -MAX_LEVEL) level--;
                        apply(pca, channel, fwdSign, level, logger);
                    }
                    case '5' -> {
                        level = 0;
                        apply(pca, channel, fwdSign, level, logger);
                    }
                    case 27, 'q', 'Q', -1 -> {
                        pca.setServoPulse(channel, STOP);
                        System.out.println("  [exit] Stopping " + name + ".");
                        exit = true;
                    }
                    default -> { /* ignore other keys */ }
                }
            }
            RawKey.restoreMode();

            logger.logPass(name + " (incremental)");
            System.out.println();
            System.out.println("[RESULT] PASS - " + name + " test ended.");
            return true;

        } catch (Exception e) {
            RawKey.restoreMode();
            System.out.println("[RESULT] FAIL - " + e.getMessage());
            logger.logFail(name, e.getMessage());
            return false;

        } finally {
            if (pca != null) {
                try { pca.setServoPulse(channel, STOP); } catch (Exception ignore) {}
                pca.close();
            }
            ServoSafety.removeStopHook(stopHook);
        }
    }

    private static void apply(PCA9685 pca, int channel, int fwdSign, int level,
                              AppLogger logger) throws Exception {
        int pulse = STOP + (fwdSign * level * STEP);
        pca.setServoPulse(channel, pulse);
        String dir = level > 0 ? "FORWARD" : (level < 0 ? "REVERSE" : "STOP");
        System.out.println("  level " + level + "  ->  " + dir
            + "   (CH" + channel + " = " + pulse + " us)");
        logger.log("  CH" + channel + " level " + level + " -> " + pulse + " us (" + dir + ")");
    }
}
