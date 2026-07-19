package my.boebot;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * BotConfig - Loads settings from config/boebot.properties
 *
 * Reads the hardware configuration so other classes
 * know which I2C bus, channels, and addresses to use.
 */
public class BotConfig {

    private final Properties props = new Properties();
    private boolean loaded = false;

    // Default values if the properties file is missing
    private int i2cBus = 1;
    private int i2cAddress = 0x40;
    private int pwmFrequency = 50;
    private int rightWheelChannel = 14;
    private int leftWheelChannel = 15;
    private boolean gripperEnabled = true;
    private int gripperChannel = 0;
    private boolean cameraModule3Enabled = true;
    private int cameraModule3Port = 0;
    private boolean tofEnabled = true;
    private int tofPort = 1;
    private String robotType = "boebot";
    private int lineSensorRightGpio = 5;
    private int lineSensorLeftGpio = 6;
    private int lineSensorThresholdUs = 1500;

    public BotConfig() {
        load();
    }

    private void load() {
        // Try to load from config/boebot.properties (relative to working directory)
        String[] possiblePaths = {
            "config/boebot.properties",
            "./config/boebot.properties",
            "../config/boebot.properties"
        };

        for (String path : possiblePaths) {
            try (InputStream in = new FileInputStream(path)) {
                props.load(in);
                loaded = true;
                System.out.println("[Config] Loaded from: " + path);
                break;
            } catch (Exception e) {
                // Try next path
            }
        }

        if (!loaded) {
            System.out.println("[Config] WARNING: config/boebot.properties not found. Using defaults.");
            System.out.println("[Config] Make sure you run this app from the project root folder.");
        }

        // Parse values (uses defaults if keys not found)
        i2cBus = getInt("servo.hat.i2c.bus", 1);
        i2cAddress = parseHexOrInt("servo.hat.i2c.address", 0x40);
        pwmFrequency = getInt("servo.frequency.hz", 50);
        rightWheelChannel = getInt("right.wheel.channel", 14);
        leftWheelChannel = getInt("left.wheel.channel", 15);
        gripperEnabled = getBoolean("gripper.enabled", true);
        gripperChannel = getInt("gripper.channel", 0);
        cameraModule3Enabled = getBoolean("camera.module3.enabled", true);
        cameraModule3Port = getInt("camera.module3.port", 0);
        tofEnabled = getBoolean("tof.enabled", true);
        tofPort = getInt("tof.port", 1);
        robotType = props.getProperty("robot.type", "boebot").trim().toLowerCase();
        lineSensorRightGpio = getInt("line.sensor.right.gpio", 5);
        lineSensorLeftGpio = getInt("line.sensor.left.gpio", 6);
        lineSensorThresholdUs = getInt("line.sensor.threshold.us", 1500);
    }

    private int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val.trim());
    }

    // Handles both "0x40" and "64" formats for the I2C address
    private int parseHexOrInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        val = val.trim();
        try {
            if (val.startsWith("0x") || val.startsWith("0X")) {
                return Integer.parseInt(val.substring(2), 16);
            }
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // --- Getters ---

    public int getI2cBus() { return i2cBus; }
    public int getI2cAddress() { return i2cAddress; }
    public int getPwmFrequency() { return pwmFrequency; }
    public int getRightWheelChannel() { return rightWheelChannel; }
    public int getLeftWheelChannel() { return leftWheelChannel; }
    public boolean isGripperEnabled() { return gripperEnabled; }
    public int getGripperChannel() { return gripperChannel; }
    public boolean isCameraModule3Enabled() { return cameraModule3Enabled; }
    public int getCameraModule3Port() { return cameraModule3Port; }
    public boolean isTofEnabled() { return tofEnabled; }
    public int getTofPort() { return tofPort; }
    public String getRobotType() { return robotType; }
    public int getLineSensorRightGpio() { return lineSensorRightGpio; }
    public int getLineSensorLeftGpio() { return lineSensorLeftGpio; }
    public int getLineSensorThresholdUs() { return lineSensorThresholdUs; }

    public void printSummary() {
        System.out.println("  I2C Bus         : " + i2cBus);
        System.out.println("  I2C Address     : 0x" + Integer.toHexString(i2cAddress).toUpperCase());
        System.out.println("  PWM Frequency   : " + pwmFrequency + " Hz");
        System.out.println("  Right Wheel     : Servo HAT channel " + rightWheelChannel);
        System.out.println("  Left Wheel      : Servo HAT channel " + leftWheelChannel);
        System.out.println("  Gripper Enabled : " + gripperEnabled + " (Servo HAT channel " + gripperChannel + ")");
        System.out.println("  Camera Module 3 : enabled=" + cameraModule3Enabled + " port=" + cameraModule3Port);
        System.out.println("  ToF Camera      : enabled=" + tofEnabled + " port=" + tofPort);
    }
}
