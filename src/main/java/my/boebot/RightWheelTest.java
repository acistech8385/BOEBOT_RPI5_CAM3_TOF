package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * RightWheelTest - Menu Option 5: interactive right wheel servo on channel 14.
 *
 * Incremental speed control (8 = faster forward, 2 = faster reverse, 5 = stop,
 * ESC/q = exit). The right wheel's forward direction is the lower pulse, so the
 * forward sign is -1. See {@link InteractiveWheel}.
 */
public class RightWheelTest {

    private static final int RIGHT_WHEEL_CH = 14;
    private static final int FORWARD_SIGN   = -1;  // right wheel: forward = lower pulse

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        return InteractiveWheel.run(logger, config, pi4j,
            RIGHT_WHEEL_CH, FORWARD_SIGN, "Right Wheel CH14", 5);
    }
}
