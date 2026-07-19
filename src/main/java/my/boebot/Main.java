package my.boebot;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import java.util.Scanner;

/**
 * Main - Entry point for BOEBOT RPi5 Hardware Test App.
 *
 * This app runs directly on the Raspberry Pi 5.
 * No IP address, username, or password is needed.
 *
 * Menu options:
 *   1  - System info
 *   2  - Check I2C Servo HAT
 *   3  - Test PCA9685 Servo HAT
 *   4  - Calibrate wheel neutral (hold 1500 us)
 *   5  - Right wheel CH14 (incremental)
 *   6  - Left wheel CH15 (incremental)
 *   7  - Both wheels auto sequence
 *   8  - Remote control (8 fwd 2 back 4 left 6 right 5 stop)
 *   9  - MG90S gripper CH0
 *   10 - Camera Module 3 detection CAM0
 *   11 - Camera Module 3 still capture CAM0
 *   12 - Camera Module 3 live preview CAM0
 *   13 - ArduCam ToF SDK detection CAM1
 *   14 - ArduCam ToF capture/save CAM1
 *   15 - ArduCam ToF live preview CAM1
 *   16 - Dual camera live view (CAM0 RGB + CAM1 ToF)
 *   17 - Full test BOEBOT (drive + dual camera + gripper)
 *   18 - Full test SUMOBOT (drive + dual camera)
 *   19 - Setup/repair Remote Access SSH + XRDP
 *   20 - Show IP address and remote status
 *   0  - Exit
 */
public class Main {

    public static void main(String[] args) {

        System.out.println();
        System.out.println("====================================");
        System.out.println(" BOEBOT RPi5 Hardware Test App");
        System.out.println("====================================");
        System.out.println(" Robot : BOEBOT");
        System.out.println(" Board : Raspberry Pi 5");
        System.out.println(" HAT   : PCA9685 Servo HAT (I2C 0x40)");
        System.out.println("====================================");
        System.out.println();

        BotConfig config = new BotConfig();
        AppLogger logger = new AppLogger();
        logger.logSystemInfo();

        Context pi4j = null;
        try {
            System.out.println("[Pi4J] Initializing Pi4J...");
            pi4j = Pi4J.newAutoContext();
            System.out.println("[Pi4J] Pi4J initialized successfully.");
        } catch (Exception e) {
            System.out.println("[Pi4J] WARNING: Pi4J could not initialize.");
            System.out.println("[Pi4J] Reason: " + e.getMessage());
            System.out.println("[Pi4J] I2C tests will not be available.");
            System.out.println("[Pi4J] (This is expected if not running on Raspberry Pi.)");
            logger.log("[Pi4J] Init failed: " + e.getMessage());
        }
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Enter choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1"  -> SystemInfoTest.run(logger);
                case "2"  -> I2CDetectTest.run(logger, config);
                case "3"  -> PCA9685InitTest.run(logger, config, pi4j);
                case "4"  -> CalibrateNeutralTest.run(logger, config, pi4j, scanner);
                case "5"  -> RightWheelTest.run(logger, config, pi4j, scanner);
                case "6"  -> LeftWheelTest.run(logger, config, pi4j, scanner);
                case "7"  -> BothWheelsTest.run(logger, config, pi4j, scanner);
                case "8"  -> DriveControlTest.run(logger, config, pi4j, scanner);
                case "9"  -> GripperTest.run(logger, config, pi4j, scanner);
                case "10" -> CameraModule3DetectTest.run(logger, config);
                case "11" -> CameraModule3Test.run(logger, config);
                case "12" -> CameraModule3PreviewTest.run(logger, config);
                case "13" -> ToFCameraTest.run(logger, config);
                case "14" -> ToFCaptureTest.run(logger, config);
                case "15" -> ToFPreviewTest.run(logger, config);
                case "16" -> DualCameraViewTest.run(logger, config);
                case "17" -> FullDriveCameraTest.run(logger, config, pi4j, scanner, "boebot");
                case "18" -> FullDriveCameraTest.run(logger, config, pi4j, scanner, "sumobot");
                case "19" -> RemoteAccessSetupTest.run(logger, scanner);
                case "20" -> RemoteStatusTest.run(logger);
                case "0"  -> {
                    System.out.println();
                    System.out.println("Exiting BOEBOT Hardware Test App.");
                    System.out.println("Log file saved: " + logger.getLogFilePath());
                    running = false;
                }
                default -> {
                    System.out.println("Invalid choice: \"" + input + "\"");
                    System.out.println("Please enter a number from the menu (0-20).");
                }
            }

            if (running) {
                System.out.println();
                System.out.println("Press any key to return to menu...");
                // Single keypress (no Enter needed). Falls back to a line read
                // if the terminal is not a TTY (raw mode unavailable).
                RawKey.enableRawMode();
                int k = RawKey.readKey();
                // Discard any trailing bytes of a multi-byte key (e.g. an
                // arrow key sends ESC '[' 'A'..'D') so they don't leak into
                // the next menu prompt's Scanner.nextLine() as e.g. "[B9".
                RawKey.drainPending();
                RawKey.restoreMode();
                if (k == -1) {
                    // raw mode not active - wait for Enter instead
                    scanner.nextLine();
                }
            }
        }

        logger.close();

        if (pi4j != null) {
            try { pi4j.shutdown(); } catch (Exception e) {}
        }

        scanner.close();
        System.out.println("Goodbye.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("====================================");
        System.out.println(" BOEBOT RPi5 Hardware Test App v2.7");
        System.out.println("====================================");
        System.out.println("1  - System info");
        System.out.println("2  - Check I2C Servo HAT");
        System.out.println("3  - Test PCA9685 Servo HAT");
        System.out.println("4  - Calibrate wheel neutral (1500us)");
        System.out.println("5  - Right wheel CH14  (8=fwd+ 2=rev+ 5=stop ESC)");
        System.out.println("6  - Left wheel CH15   (8=fwd+ 2=rev+ 5=stop ESC)");
        System.out.println("7  - Both wheels auto sequence (fwd rev TurnR TurnL)");
        System.out.println("8  - Remote control    (8 fwd 2 back 4 left 6 right 5 stop)");
        System.out.println("9  - Gripper CH0       (8=close 2=open 5=stop ESC)");
        System.out.println("10 - Camera Module 3 detection CAM0");
        System.out.println("11 - Camera Module 3 still capture CAM0");
        System.out.println("12 - Camera Module 3 live preview CAM0");
        System.out.println("13 - ArduCam ToF SDK detection CAM1");
        System.out.println("14 - ArduCam ToF capture/save CAM1");
        System.out.println("15 - ArduCam ToF live preview CAM1");
        System.out.println("16 - Dual camera live view CAM0+CAM1");
        System.out.println("17 - Full test BOEBOT (drive + cam + gripper)");
        System.out.println("18 - Full test SUMOBOT (drive + cam)");
        System.out.println("19 - Setup/repair Remote Access SSH + XRDP");
        System.out.println("20 - Show IP address and remote status");
        System.out.println("0  - Exit");
        System.out.println("====================================");
    }
}
