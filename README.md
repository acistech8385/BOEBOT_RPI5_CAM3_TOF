# BOEBOT RPi5 Hardware Test App

A Java 17 Maven command-line app that runs **directly on the Raspberry Pi 5** to test all BOEBOT hardware.

> **This app does not need an IP address, username, or password.**  
> It runs locally on the Raspberry Pi — you SSH in once, clone the repo, and run the app.  
> Do not install Claude Code on the Raspberry Pi. Use the normal Raspberry Pi terminal only.

---

## Hardware

| Part | Details |
|------|---------|
| Robot | BOEBOT |
| Board | Raspberry Pi 5 Model B |
| Servo HAT | PCA9685 — I2C bus 1, address 0x40, 50 Hz |
| Right Wheel | Parallax continuous rotation servo — Servo HAT channel 14 |
| Left Wheel | Parallax continuous rotation servo — Servo HAT channel 15 |
| Gripper | MG90S servo — Servo HAT channel 0 |
| Camera | Raspberry Pi Camera Module 3 — CAM/DISP 0 |
| ToF Camera | ArduCam ToF Camera — CAM/DISP 1 |

---

## Menu

```
====================================
 BOEBOT RPi5 Hardware Test App v1.8
====================================
1  - System info
2  - Check I2C Servo HAT
3  - Test PCA9685 Servo HAT
4  - Test right wheel servo CH14
5  - Test left wheel servo CH15
6  - Test both wheel servos
7  - Test MG90S gripper CH0
8  - Camera Module 3 still capture CAM0
9  - Camera Module 3 live preview CAM0
10 - ArduCam ToF SDK detection CAM1
11 - ArduCam ToF live preview CAM1
12 - ArduCam ToF capture/save CAM1
13 - Full safe hardware test
0  - Exit
====================================
```

---

## Raspberry Pi Setup (First Time)

Do all of these steps in the **Raspberry Pi terminal** (SSH or local).  
Do **not** run these on Windows.

### Step 1 — Create a project folder and clone the repo

```bash
mkdir -p ~/BOEBOT_RPI5_CAM3_TOF
cd ~/BOEBOT_RPI5_CAM3_TOF
git clone -b master https://github.com/acistech8385/BOEBOT_RPI5_CAM3_TOF.git
cd BOEBOT_RPI5_CAM3_TOF
```

Your project path will be: `~/BOEBOT_RPI5_CAM3_TOF/BOEBOT_RPI5_CAM3_TOF`

### Step 2 — Make scripts executable

```bash
chmod +x scripts/install_boebot.sh scripts/run_boebot_test.sh
```

### Step 3 — Run the install script

The install script is **idempotent** — safe to run more than once.  
It installs: Java 17, Maven, i2c-tools, rpicam-apps, Python3, numpy, opencv-python, ArduCam ToF SDK.

```bash
./scripts/install_boebot.sh
```

### Step 4 — Reboot if needed

If the install script says **REBOOT REQUIRED**, reboot now:

```bash
sudo reboot
```

After reboot, re-open a terminal and go back to the project:

```bash
cd ~/BOEBOT_RPI5_CAM3_TOF/BOEBOT_RPI5_CAM3_TOF
```

### Step 5 — Verify I2C sees the PCA9685 Servo HAT

```bash
sudo i2cdetect -y 1
```

**Expected output** — look for `40` in the grid at row `40:`:

```
     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
00:          -- -- -- -- -- -- -- -- -- -- -- -- --
...
40: 40 -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
...
```

If you do **not** see `40`:
- Check the PCA9685 Servo HAT is firmly seated on the GPIO pins.
- Check power: the HAT may need 5V via its dedicated power connector.
- Check that `/dev/i2c-1` exists: `ls /dev/i2c*`

### Step 6 — Run the hardware test app

```bash
./scripts/run_boebot_test.sh
```

The script builds the app with Maven (first run downloads dependencies) and launches the menu.

---

## Getting Updates (After First Setup)

When code is updated on GitHub:

```bash
cd ~/BOEBOT_RPI5_CAM3_TOF/BOEBOT_RPI5_CAM3_TOF
git pull
./scripts/run_boebot_test.sh
```

If there are new install requirements, run the install script again — it is safe to run multiple times:

```bash
./scripts/install_boebot.sh
```

---

## Safety — Wheel Tests

> **ALWAYS lift the BOEBOT wheels off the ground before running any wheel servo test.**

Before any wheel test (options 4, 5, 6, and the wheel section of option 13),  
the app will ask you to type **exactly**:

```
WHEELS_LIFTED
```

If you type anything else, the wheel test is **cancelled immediately**.

Wheel movement duration is limited to **1 second maximum** per direction.

---

## Camera Tests (CAM0 — Camera Module 3)

### Option 8 — Still Capture

Captures a single image using `rpicam-still` (Bookworm) or `libcamera-still` (Bullseye).  
Works in SSH sessions — **no display needed**.  
Image is saved to `logs/<hostname>/cam3_YYYYMMDD_HHMMSS.jpg`.

On success:
```
IMAGE SAVED:
Folder:    /home/faix/.../logs/boebot-1
File:      cam3_20260620_203300.jpg
Full path: /home/faix/.../logs/boebot-1/cam3_20260620_203300.jpg
```

### Option 9 — Live Preview

Opens a live camera preview window on the screen.  
**Requires a display** — HDMI/DSI screen, VNC, or X11 forwarding (`ssh -X`).  
**Remote Desktop (RDP) does NOT export DISPLAY** — use VNC or a physical screen.  
Close the camera window to return to the menu (or press Ctrl+C).

If no display is detected, the app prints:
```
LIVE PREVIEW NOT AVAILABLE: no display detected. Use still capture test instead.
```

---

## ToF Camera Tests (CAM1 — ArduCam ToF)

### Option 10 — SDK Detection

Checks if the ArduCam ToF SDK is installed at `~/Arducam_tof_camera`.  
Checks Python 3, lists example files, and verifies `ArducamDepthCamera` is importable.  
**No camera is opened.** PASS = SDK ready, FAIL = missing components with fix instructions.

### Option 11 — Live Preview

Opens a live depth + amplitude preview window using Python + OpenCV.  
**Requires a display** — HDMI/DSI screen, VNC, or X11 forwarding.  
**Remote Desktop (RDP) does NOT export DISPLAY** — use VNC or a physical screen.

Displays:
- **Left panel**: depth map (JET colourmap, 0–4 m range)
- **Right panel**: amplitude / confidence map (greyscale)

Close the window or press **Q / ESC** to stop. Or press Ctrl+C to force-stop.

If no display is detected:
```
LIVE PREVIEW NOT AVAILABLE: no display detected. Use still capture test instead.
```

### Option 12 — Capture / Save

Captures a single ToF frame and saves output files.  
**No display needed** — works in headless SSH sessions.

Files saved to `logs/<hostname>/`:

| File | Contents |
|------|----------|
| `tof_depth_YYYYMMDD_HHMMSS.csv` | Raw depth values in mm (always saved) |
| `tof_depth_YYYYMMDD_HHMMSS.png` | Depth as JET colourmap (requires cv2) |
| `tof_confidence_YYYYMMDD_HHMMSS.png` | Confidence greyscale (requires cv2) |

On success:
```
TOF OUTPUT SAVED:
Folder: /home/faix/.../logs/boebot-1
Files:
  - tof_depth_20260620_203300.csv
  - tof_depth_20260620_203300.png
  - tof_confidence_20260620_203300.png
Full paths:
  - /home/faix/.../tof_depth_20260620_203300.csv
  - ...
```

---

## Log Files

Logs are saved automatically to:

```
logs/<hostname>/boebot_<timestamp>.log
```

Each test prints **PASS** or **FAIL** in both the console and the log file.  
System info, OS version, Java version, and hostname are logged at startup.  
Still images and ToF output files are saved to the same `logs/<hostname>/` folder.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `i2cdetect` shows no devices | Run `./scripts/install_boebot.sh` or: `sudo raspi-config` → Interface Options → I2C |
| `/dev/i2c-1` missing after enabling I2C | Reboot: `sudo reboot` |
| PCA9685 not found at 0x40 | Check HAT is seated on GPIO pins, check HAT power connector |
| `rpicam-still` not found | `sudo apt-get install -y rpicam-apps` |
| Camera: no image created | Check ribbon cable in CAM0, check: `rpicam-still --list-cameras` |
| Preview: no window appears | Need a display or VNC session; RDP does not export DISPLAY |
| ArduCam ToF not found | Run `./scripts/install_boebot.sh` to auto-clone the SDK |
| `ArducamDepthCamera` not importable | Run: `bash ~/Arducam_tof_camera/Install_dependencies.sh` |
| ToF capture: no PNG output | Install cv2: `sudo apt-get install -y python3-opencv` |
| numpy not found | `pip3 install numpy` |
| I2C permission error | `sudo usermod -aG i2c $USER` then log out and back in |
| Java not found | `sudo apt-get install -y openjdk-17-jdk` |
| Maven not found | `sudo apt-get install -y maven` |
| Python3 not found | `sudo apt-get install -y python3` |

---

## Project Structure

```
BOEBOT_RPI5_CAM3_TOF/
├── README.md
├── pom.xml
├── config/
│   └── boebot.properties              Hardware configuration
├── scripts/
│   ├── install_boebot.sh              Install all dependencies on Raspberry Pi
│   └── run_boebot_test.sh             Build and run the app
└── src/main/java/my/boebot/
    ├── Main.java                       Entry point and menu loop
    ├── BotConfig.java                  Loads config/boebot.properties
    ├── AppLogger.java                  Saves logs to logs/<hostname>/
    ├── PCA9685.java                    I2C driver for PCA9685 Servo HAT
    ├── WheelSafety.java                WHEELS_LIFTED safety confirmation
    ├── SystemInfoTest.java             Option 1:  System information
    ├── I2CDetectTest.java              Option 2:  I2C detect / confirm 0x40
    ├── PCA9685InitTest.java            Option 3:  PCA9685 initialization
    ├── RightWheelTest.java             Option 4:  Right wheel servo CH14
    ├── LeftWheelTest.java              Option 5:  Left wheel servo CH15
    ├── BothWheelsTest.java             Option 6:  Both wheel servos
    ├── GripperTest.java                Option 7:  MG90S gripper servo CH0
    ├── CameraModule3Test.java          Option 8:  Camera Module 3 still capture
    ├── CameraModule3PreviewTest.java   Option 9:  Camera Module 3 live preview
    ├── ToFCameraTest.java              Option 10: ArduCam ToF SDK detection
    ├── ToFPreviewTest.java             Option 11: ArduCam ToF live preview
    ├── ToFCaptureTest.java             Option 12: ArduCam ToF capture/save
    └── FullHardwareTest.java           Option 13: Full safe hardware test
```

---

## Notes

- This app runs **directly on the Raspberry Pi**. No SSH credentials needed inside the app.
- **Do not install Claude Code on the Raspberry Pi.** Use the normal Raspberry Pi terminal.
- BOEBOT has no line sensors — line sensor code is not included.
- No Sumobot code is included.
- Built with Java 17 and Pi4J 2.6.0 (I2C via LinuxFS provider).
