package my.boebot;

import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * LeftWheelTest - Menu Option 8: interactive left wheel servo on channel 15.
 *
 * Incremental speed control (8 = faster forward, 2 = faster reverse, 5 = stop,
 * ESC/q = exit). The left wheel faces the opposite way to the right, so its
 * forward direction is the higher pulse and the forward sign is +1.
 * See {@link InteractiveWheel}.
 */
public class LeftWheelTest {

    private static final int LEFT_WHEEL_CH = 15;
    private static final int FORWARD_SIGN  = 1;  // left wheel: forward = higher pulse

    public static boolean run(AppLogger logger, BotConfig config,
                               Context pi4j, Scanner scanner) {
        return InteractiveWheel.run(logger, config, pi4j,
            LEFT_WHEEL_CH, FORWARD_SIGN, "Left Wheel CH15", 8);
    }
}
