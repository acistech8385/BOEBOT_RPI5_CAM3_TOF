# BOEBOT RPi5 Hardware Test App

A Java 17 Maven command-line app that runs **directly on the Raspberry Pi 5** to test all BOEBOT hardware.

> **This app does not need an IP address, username, or password.**  
> It runs locally on the Raspberry Pi — you SSH in once, clone the repo, and run the app.

---

## Hardware

| Part | Details |
|------|---------|
| Robot | BOEBOT |
| Board | Raspberry Pi 5 |
| Servo HAT | PCA9685 — I2C bus 1, address 0x40 |
| Right Wheel | Parallax continuous rotation servo — Servo HAT channel 14 |
| Left Wheel | Parallax continuous rotation servo — Servo HAT channel 15 |
| Gripper | MG90S servo — Servo HAT channel 0 |
| Camera | Raspberry Pi Camera Module 3 — CAM/DISP 0 |
| ToF Camera | ArduCam ToF Camera — CAM/DISP 1 |

---

## Menu

```
====================================
 BOEBOT RPi5 Hardware Test App
====================================
1  - System info
2  - Check I2C Servo HAT
3  - Test PCA9685 Servo HAT
4  - Test right wheel servo CH14
5  - Test left wheel servo CH15
6  - Test both wheel servos
7  - Test MG90S gripper CH0
8  - Test Camera Module 3 CAM0
9  - Test ArduCam ToF CAM1
10 - Full safe hardware test
0  - Exit
====================================
```

---

## Setup Instructions (on the Raspberry Pi)

### Step 1 — Clone the repository

```bash
git clone https://github.com/acistech8385/BOEBOT_RPI5_CAM3_TOF.git
```

### Step 2 — Enter the project folder

```bash
cd BOEBOT_RPI5_CAM3_TOF
```

### Step 3 — Make the scripts executable

```bash
chmod +x scripts/install_boebot.sh scripts/run_boebot_test.sh
```

### Step 4 — Run the install script

This installs Java 17, Maven, i2c-tools, and rpicam-apps.  
Safe to run multiple times.

```bash
./scripts/install_boebot.sh
```

After the install script finishes, **reboot** if I2C was not already enabled:

```bash
sudo reboot
```

### Step 5 — Check I2C manually

After reboot, verify that the PCA9685 Servo HAT is detected:

```bash
sudo i2cdetect -y 1
```

**Expected output** — you should see `40` in the grid:

```
     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
00:          -- -- -- -- -- -- -- -- -- -- -- -- --
...
40: 40 -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
...
```

If you do **not** see `40`:
- Check that the PCA9685 Servo HAT is firmly seated on the GPIO pins.
- Check power: the HAT may need 5V via its dedicated power connector.
- Check the I2C address jumpers (A0-A5 pads) on the HAT — all open = 0x40.

### Step 6 — Run the hardware test app

```bash
./scripts/run_boebot_test.sh
```

The script builds the app with Maven and runs it.

---

## Safety — Wheel Tests

> **ALWAYS lift the BOEBOT wheels off the ground before running any wheel servo test.**

Before any wheel test, the app will ask you to type exactly:

```
WHEELS_LIFTED
```

If you type anything else (or press Enter), the wheel test is **cancelled**.

Wheel movement duration is limited to **1 second maximum** per direction.

---

## Log Files

Logs are saved automatically in:

```
logs/<hostname>/boebot_<timestamp>.log
```

Each test prints **PASS** or **FAIL** and this is recorded in the log file.  
System info, OS version, Java version, and hostname are logged at startup.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `i2cdetect` shows no devices | Enable I2C: `sudo raspi-config` → Interface Options → I2C |
| PCA9685 not found at 0x40 | Check HAT is seated on GPIO pins, check power connector |
| `rpicam-still` not found | `sudo apt-get install -y rpicam-apps` |
| ArduCam ToF not found | Install SDK from: https://github.com/ArduCAM/Arducam_tof_camera |
| I2C permission error | `sudo usermod -aG i2c $USER` then log out and back in |
| Java not found | `sudo apt-get install -y openjdk-17-jdk` |
| Maven not found | `sudo apt-get install -y maven` |

---

## Project Structure

```
BOEBOT_RPI5_CAM3_TOF/
├── README.md
├── pom.xml
├── config/
│   └── boebot.properties         Hardware configuration
├── scripts/
│   ├── install_boebot.sh         Install dependencies on RPi
│   └── run_boebot_test.sh        Build and run the app
└── src/main/java/my/boebot/
    ├── Main.java                  Entry point and menu loop
    ├── BotConfig.java             Loads config/boebot.properties
    ├── AppLogger.java             Saves test results to log files
    ├── PCA9685.java               I2C driver for PCA9685 Servo HAT
    ├── WheelSafety.java           WHEELS_LIFTED safety confirmation
    ├── SystemInfoTest.java        Test 1: System information
    ├── I2CDetectTest.java         Test 2: I2C detect / check 0x40
    ├── PCA9685InitTest.java       Test 3: PCA9685 initialization
    ├── RightWheelTest.java        Test 4: Right wheel servo CH14
    ├── LeftWheelTest.java         Test 5: Left wheel servo CH15
    ├── BothWheelsTest.java        Test 6: Both wheel servos
    ├── GripperTest.java           Test 7: MG90S gripper servo CH0
    ├── CameraModule3Test.java     Test 8: Camera Module 3 CAM0
    ├── ToFCameraTest.java         Test 9: ArduCam ToF CAM1
    └── FullHardwareTest.java      Test 10: Full safe hardware test
```

---

## Notes

- This app does **not** require SSH credentials inside the app.
  You SSH into the Raspberry Pi once, then the app runs locally.
- The ArduCam ToF test is a **detection check only** — it does not start the camera.
- BOEBOT has no line sensors — line sensor code is not included.
- Built with Java 17 and Pi4J 2.x.
