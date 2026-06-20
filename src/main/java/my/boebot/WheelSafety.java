package my.boebot;

import java.util.Scanner;

/**
 * WheelSafety - Safety confirmation before any wheel movement test.
 *
 * Before moving the BOEBOT wheels, the user MUST type exactly:
 *   WHEELS_LIFTED
 *
 * This confirms that the robot is lifted off the ground
 * so wheels can spin safely without the robot driving away.
 */
public class WheelSafety {

    private static final String REQUIRED_CONFIRMATION = "WHEELS_LIFTED";

    /**
     * Asks the user to confirm wheels are lifted before a wheel test.
     *
     * @param scanner  the console input scanner
     * @return true if the user typed WHEELS_LIFTED exactly, false otherwise
     */
    public static boolean confirmWheelsLifted(Scanner scanner) {
        System.out.println();
        System.out.println("*** WHEEL MOVEMENT SAFETY CHECK ***");
        System.out.println("-----------------------------------");
        System.out.println("WARNING: This test will move the BOEBOT wheels.");
        System.out.println();
        System.out.println("Before proceeding:");
        System.out.println("  - Lift the BOEBOT off the ground.");
        System.out.println("  - Make sure the wheels can spin freely.");
        System.out.println("  - Keep hands away from the wheels.");
        System.out.println();
        System.out.println("Type exactly:  WHEELS_LIFTED   then press Enter.");
        System.out.println("Type anything else (or just Enter) to CANCEL.");
        System.out.println();
        System.out.print("Your input: ");

        String input = scanner.nextLine().trim();

        if (REQUIRED_CONFIRMATION.equals(input)) {
            System.out.println("[Safety] Confirmed. Wheels are lifted. Proceeding with wheel test.");
            return true;
        } else {
            System.out.println("[Safety] Cancelled. You typed: \"" + input + "\"");
            System.out.println("[Safety] Wheel test skipped for safety.");
            return false;
        }
    }
}
