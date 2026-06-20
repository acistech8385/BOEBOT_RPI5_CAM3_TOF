package my.boebot;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * PCA9685 - Driver for the PCA9685 16-channel PWM Servo HAT.
 *
 * The PCA9685 is connected via I2C and controls servo motors
 * by generating PWM signals on each of its 16 channels.
 *
 * Hardware:
 *   I2C Bus     : 1  (/dev/i2c-1 on Raspberry Pi)
 *   I2C Address : 0x40
 *   PWM Freq    : 50 Hz (standard for RC servos)
 *
 * Channels used:
 *   Channel 0  = MG90S gripper servo
 *   Channel 14 = Right wheel (Parallax continuous rotation servo)
 *   Channel 15 = Left wheel  (Parallax continuous rotation servo)
 */
public class PCA9685 {

    // PCA9685 register addresses
    private static final int REG_MODE1   = 0x00;
    private static final int REG_MODE2   = 0x01;
    private static final int REG_PRESCALE = 0xFE;

    // Base register for channel 0 PWM data (each channel uses 4 bytes)
    // LED0_ON_L = 0x06, LED0_ON_H = 0x07, LED0_OFF_L = 0x08, LED0_OFF_H = 0x09
    // Channel n base = 0x06 + (n * 4)
    private static final int LED0_ON_L = 0x06;

    // PCA9685 internal oscillator frequency = 25 MHz
    private static final double OSCILLATOR_FREQ = 25_000_000.0;

    // PWM resolution = 4096 steps (12-bit)
    private static final int PWM_RESOLUTION = 4096;

    // PWM period in microseconds at 50 Hz = 20,000 us
    private static final double PWM_PERIOD_US = 20_000.0;

    private I2C i2c;
    private boolean initialized = false;
    private int pwmFrequencyHz = 50;

    /**
     * Creates and initializes the PCA9685 at the given I2C bus and address.
     *
     * @param pi4j    the Pi4J context (create with Pi4J.newAutoContext())
     * @param bus     I2C bus number (1 for Raspberry Pi)
     * @param address I2C address (0x40 for default PCA9685)
     * @throws Exception if I2C cannot be opened (e.g., not on Raspberry Pi)
     */
    public PCA9685(Context pi4j, int bus, int address) throws Exception {
        // Build the I2C configuration
        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
            .id("PCA9685")
            .name("PCA9685 Servo HAT")
            .bus(bus)
            .device(address)
            .build();

        // Create the I2C device connection
        i2c = pi4j.create(i2cConfig);
        System.out.println("[PCA9685] I2C device opened on bus " + bus + " at address 0x"
            + Integer.toHexString(address).toUpperCase());
    }

    /**
     * Initializes the PCA9685 and sets the PWM frequency.
     * Must be called after the constructor before using servos.
     *
     * @param frequencyHz PWM frequency in Hz (50 for standard RC servos)
     * @throws Exception if I2C write fails
     */
    public void initialize(int frequencyHz) throws Exception {
        this.pwmFrequencyHz = frequencyHz;

        // Step 1: Reset the PCA9685 (write 0x00 to MODE1)
        // This wakes up the chip and clears any previous state.
        writeReg(REG_MODE1, 0x00);
        Thread.sleep(10);

        // Step 2: Enter sleep mode so we can set the prescaler
        // Bit 4 (SLEEP) must be set before changing PRESCALE
        writeReg(REG_MODE1, 0x10);
        Thread.sleep(5);

        // Step 3: Calculate and write the prescale value
        // prescale = round(oscillator / (resolution * freq)) - 1
        int prescale = (int) Math.round(OSCILLATOR_FREQ / (PWM_RESOLUTION * frequencyHz)) - 1;
        System.out.println("[PCA9685] Setting prescale to " + prescale
            + " for " + frequencyHz + " Hz");
        writeReg(REG_PRESCALE, prescale);

        // Step 4: Wake up (clear sleep bit)
        writeReg(REG_MODE1, 0x00);
        Thread.sleep(5);

        // Step 5: Enable auto-increment mode for sequential register writes
        // Bit 5 (AI) in MODE1 = auto-increment
        writeReg(REG_MODE1, 0x20);
        Thread.sleep(5);

        initialized = true;
        System.out.println("[PCA9685] Initialized at " + frequencyHz + " Hz");
    }

    /**
     * Sets a servo channel to a specific pulse width in microseconds.
     *
     * Typical servo values:
     *   1500 us = neutral / stop (for continuous rotation)
     *   1100 us = gripper closed (for MG90S position servo)
     *   1900 us = gripper open
     *
     * @param channel  servo channel (0-15)
     * @param pulseUs  pulse width in microseconds
     * @throws Exception if I2C write fails
     */
    public void setServoPulse(int channel, int pulseUs) throws Exception {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("Channel must be 0-15, got: " + channel);
        }

        // Convert pulse width (us) to PWM tick count
        // tick = (pulseUs / period_us) * resolution
        int offTick = (int) Math.round((pulseUs / PWM_PERIOD_US) * PWM_RESOLUTION);

        // Set ON at tick 0, OFF at the calculated tick
        setPwmTicks(channel, 0, offTick);
    }

    /**
     * Sets the raw PWM on/off tick values for a channel.
     *
     * @param channel  channel number (0-15)
     * @param onTick   tick at which the signal goes HIGH (usually 0)
     * @param offTick  tick at which the signal goes LOW (0-4095)
     */
    private void setPwmTicks(int channel, int onTick, int offTick) throws Exception {
        // Base register for this channel
        int base = LED0_ON_L + (channel * 4);

        // Write 4 bytes: ON_L, ON_H, OFF_L, OFF_H
        writeReg(base + 0, onTick & 0xFF);           // ON_L (low byte of ON tick)
        writeReg(base + 1, (onTick >> 8) & 0x0F);    // ON_H (high nibble of ON tick)
        writeReg(base + 2, offTick & 0xFF);           // OFF_L (low byte of OFF tick)
        writeReg(base + 3, (offTick >> 8) & 0x0F);   // OFF_H (high nibble of OFF tick)
    }

    /**
     * Stops all PWM output on a channel (sets the servo to neutral/stop).
     * Uses 1500 us (neutral) for continuous rotation servos.
     *
     * @param channel  servo channel to stop
     */
    public void stopServo(int channel) throws Exception {
        setServoPulse(channel, 1500);
    }

    /**
     * Writes one byte to a PCA9685 register over I2C.
     * Format: [register_address, value]
     */
    private void writeReg(int register, int value) throws Exception {
        byte[] data = new byte[]{
            (byte) (register & 0xFF),
            (byte) (value & 0xFF)
        };
        i2c.write(data, 0, data.length);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Closes the I2C connection. */
    public void close() {
        try {
            if (i2c != null) {
                i2c.close();
                System.out.println("[PCA9685] I2C connection closed.");
            }
        } catch (Exception e) {
            System.out.println("[PCA9685] Warning closing I2C: " + e.getMessage());
        }
    }
}
