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
 BOEBOT RPi5 Hardware Test App v2.7
====================================
1  - System info
2  - Check I2C Servo HAT
3  - Test PCA9685 Servo HAT
4  - Calibrate wheel neutral (1500us)
5  - Right wheel CH14  (8=fwd+ 2=rev+ 5=stop ESC)
6  - Left wheel CH15   (8=fwd+ 2=rev+ 5=stop ESC)
7  - Both wheels auto sequence (fwd rev TurnR TurnL)
8  - Remote control    (8 fwd 2 back 4 left 6 right 5 stop)
9  - Gripper CH0       (8=close 2=open 5=stop ESC)
10 - Camera Module 3 detection CAM0
11 - Camera Module 3 still capture CAM0
12 - Camera Module 3 live preview CAM0
13 - ArduCam ToF SDK detection CAM1
14 - ArduCam ToF capture/save CAM1
15 - ArduCam ToF live preview CAM1
16 - Dual camera live view CAM0+CAM1
17 - Full test BOEBOT (drive + dual cam + gripper)
18 - Full test SUMOBOT (drive + dual cam)
19 - Setup/repair Remote Access SSH + XRDP
20 - Show IP address and remote status
0  - Exit
====================================
```

---

## Raspberry Pi Setup (First Time)

Do all of these steps in the **Raspberry Pi terminal** (SSH or local).  
Do **not** run these on Windows.

### Step 1 — Clone the repo (single-level folder)

```bash
cd ~
git clone -b master https://github.com/acistech8385/BOEBOT_RPI5_CAM3_TOF.git
cd BOEBOT_RPI5_CAM3_TOF
```

Your project path will be: `~/BOEBOT_RPI5_CAM3_TOF` (one level, not nested).

> Already cloned into the old nested path `~/BOEBOT_RPI5_CAM3_TOF/BOEBOT_RPI5_CAM3_TOF`?
> Move it up one level:
> ```bash
> mv ~/BOEBOT_RPI5_CAM3_TOF ~/BOEBOT_tmp && mv ~/BOEBOT_tmp/BOEBOT_RPI5_CAM3_TOF ~/BOEBOT_RPI5_CAM3_TOF && rm -rf ~/BOEBOT_tmp
> ```
> Then use `cd ~/BOEBOT_RPI5_CAM3_TOF` from now on.

### Step 2 — Make scripts executable

```bash
chmod +x scripts/install_boebot.sh scripts/run_boebot_test.sh
```

### Step 3 — Run the install script

The install script is **idempotent** — safe to run more than once.  
It installs: a **Java JDK** (17 on Bookworm, 21 on Trixie — both build the app),
Maven, i2c-tools, rpicam-apps, Python3, numpy, opencv-python, python3-picamera2,
ArduCam ToF SDK. It also **enables I2C** and **writes the dual-camera overlays** to
`config.txt` (`camera_auto_detect=0`, `dtoverlay=imx708,cam0`,
`dtoverlay=arducam-pivariety,cam1`).

> The script re-normalises the camera overlays **after** the ArduCam ToF SDK
> installer runs, because that installer adds its own `arducam-pivariety,cam0`
> overlay which conflicts with Camera Module 3. So a fresh Pi needs no manual
> config.txt editing — just run the script, then reboot.

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
cd ~/BOEBOT_RPI5_CAM3_TOF
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
cd ~/BOEBOT_RPI5_CAM3_TOF
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

The servo and drive tests (options 4–9, 16) run **immediately** with no typed
confirmation — lift the wheels first. Ctrl+C is always safe (it cuts all servo
outputs).

---

## Servo Tests (interactive)

### Option 4 — Calibrate wheel neutral

Holds **both** wheel servos at 1500 µs so you can trim each one to a true stop.
Continuous-rotation servos rarely stop exactly at 1500 µs out of the box — they
creep. With this running, turn the small trim potentiometer on the side of each
servo (small hole in the case) with a fine screwdriver until that wheel stops
completely. An already-trimmed wheel stays still, so only the wheel that still
needs adjusting will be moving.

| Key | Action |
|-----|--------|
| `5` | Re-send 1500 µs to both wheels |
| `ESC` / `q` | Exit |

**Do this first.** Once both wheels stop at 1500 µs, options 5/6/7/8 drive
correctly with no code changes.

### Option 5 — Right wheel CH14 / Option 6 — Left wheel CH15 (incremental)

Drive one continuous-rotation wheel with **incremental speed** (no Enter needed):

| Key | Action |
|-----|--------|
| `8` | Faster forward (press again to speed up) |
| `2` | Faster reverse (press again to speed up) |
| `5` | Stop |
| `ESC` / `q` | Stop and exit |

Speed steps from level 0 (stop) to ±5 (~full speed). Left and right servos face
opposite ways, so each side has its own forward direction (handled in code).

### Option 7 — Both wheels, automatic sequence

Runs this sequence with both wheels, then stops:

1. Forward 2 s → pause 1 s
2. Backward 2 s → pause 2 s
3. Turn right 2 s (right wheel back, left wheel forward) → pause 1 s
4. Turn left 2 s (right wheel forward, left wheel back) → pause 1 s → stop

### Option 8 — Remote control

Drive the whole robot. The robot keeps moving until you press `5` or another
direction (no Enter needed):

| Key | Action |
|-----|--------|
| `8` | Forward |
| `2` | Backward |
| `4` | Turn left |
| `6` | Turn right |
| `5` | Stop |
| `ESC` / `q` | Stop and exit |

### Option 9 — MG90S gripper CH0

Incremental gripper control. A stalled MG90S draws ~0.6–1 A and can brown out
the board (servo freezes, SSH/RDP drops), so this test moves in small steps
within a safe range (1350–1650 µs) **and cuts the channel's PWM after each move**
— the servo travels, then relaxes, so it never *holds* a stall.

| Key | Action |
|-----|--------|
| `8` | Close a step |
| `2` | Open a step |
| `5` | Relax (cut PWM now) |
| `ESC` / `q` | Exit |

The gripper relaxing between moves is expected. Adjust `OPEN_LIMIT` /
`CLOSE_LIMIT` / `STEP` in `GripperTest.java` to match your gripper.

> If the gripper still strains or the board browns out, the Servo HAT likely
> needs its **own VIN power supply** (6–12 V) — a stalling MG90S can pull more
> current than the Pi's 5 V rail alone can provide.

### Option 17 — Full test BOEBOT / Option 18 — Full test SUMOBOT

One window shows both cameras (Camera Module 3 RGB | ArduCam ToF depth) **and**
takes the driving keys, so there is no focus fight between the terminal and the
window. Click the camera window, then drive **HOLD-TO-MOVE** — hold a direction
key to move, release it and the wheels stop (a watchdog stops the motors if no
movement key arrives for ~0.4 s):

| Key | Action |
|-----|--------|
| `8` (hold) | Forward |
| `2` (hold) | Backward |
| `4` (hold) | Turn left |
| `6` (hold) | Turn right |
| `5` | Stop |
| `Q` / `ESC` | Quit (servos stop) |

**Option 17 (BOEBOT)** adds gripper control: `o` = open, `c` = close (CH0).
**Option 18 (SUMOBOT)** is the same drive + dual camera test without gripper
keys, since the SumoBot chassis has no gripper.

**Needs a display** (HDMI/VNC/X11) — the driving keys are read by the window.
**Calibrate the wheels first (option 4)** or an untrimmed wheel will creep even
when stopped.

> Single-key control uses the Linux terminal raw mode (`stty`). Run the app in a
> real Raspberry Pi terminal (local or SSH), not a pipe, for keys to register.
> If a single key does nothing, press **Enter** after it (`8` then Enter, etc.).
>
> **Ctrl+C is safe:** a shutdown hook turns OFF all PCA9685 outputs and restores
> the terminal, so the servo stops even if you force-quit.
>
> **Wheel keeps creeping at "stop"?** The continuous-rotation servo needs
> calibration — send 1500 µs (press `5`) and turn the small potentiometer on the
> servo body until it stops completely. 1500 µs is not a guaranteed stop until
> calibrated.
>
> **Emergency stop (any time), without the app:**
> ```bash
> i2cset -y 1 0x40 0xFD 0x10    # turn OFF all servo outputs
> ```

---

## Camera Tests (CAM0 — Camera Module 3)

### Option 10 — Detection

Runs `rpicam-hello --list-cameras` and checks that the **imx708** sensor (Camera
Module 3) is listed on CAM0. No image is captured and no window opens.
PASS = camera detected, FAIL = not found (with fix hints).

### Option 11 — Still Capture

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

### Option 12 — Live Preview

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

### Option 13 — SDK Detection

Checks if the ArduCam ToF SDK is installed at `~/Arducam_tof_camera`.  
Checks Python 3, lists example files, and verifies `ArducamDepthCamera` is importable.  
**No camera is opened.** PASS = SDK ready, FAIL = missing components with fix instructions.

### Option 15 — Live Preview

Opens a live depth + amplitude preview window using Python + OpenCV.  
**Requires a display** — HDMI/DSI screen, VNC, or X11 forwarding.  
**Remote Desktop (RDP) does NOT export DISPLAY** — use VNC or a physical screen.

Displays:
- **Left panel**: depth map (JET colourmap, 0–4 m range)
- **Right panel**: amplitude / confidence map (greyscale)

Close the window (X button) or press **Q / ESC** to stop. Or press Ctrl+C to
force-stop.

If no display is detected:
```
LIVE PREVIEW NOT AVAILABLE: no display detected. Use still capture test instead.
```

### Option 14 — Capture / Save

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

## Dual Camera View (CAM0 + CAM1 together)

### Option 16 — Dual Live View

Shows **both cameras at once** in a single window, side by side:

- **Left panel**: Camera Module 3 RGB feed (CAM0, via picamera2)
- **Right panel**: ArduCam ToF depth map (CAM1, JET colourmap, via the SDK)

This proves both cameras run simultaneously without conflict.

**Requires a display** — HDMI/DSI screen, VNC, or X11 forwarding (`ssh -X`).
**Remote Desktop (RDP) does NOT export DISPLAY** — use VNC or a physical screen.
For headless checks use option 8 (CM3 capture) + option 12 (ToF capture) instead.

Close the window or press **Q / ESC** to stop. Or press Ctrl+C to force-stop.

> **Note on the ToF depth panel:** the ToF only shows colour where it gets a
> valid return within ~0.2–4 m. Pointed at empty space, a far wall (>4 m), or
> glass, the depth panel reads mostly black — this is normal, not a fault.
> Point it at a person or object 0.5–1.5 m away to see the depth gradient
> (blue = near, red = far).

Needs `python3-picamera2` in addition to `numpy` and `python3-opencv`:

```bash
sudo apt-get install -y python3-picamera2 python3-opencv
```

---

## Remote Access (SSH + Remote Desktop)

> **No IP address, username, or password is ever asked for or stored by the app.**
> The app only installs/enables services and reads the Pi's own hostname/IP live
> to print it. You log in with your normal Raspberry Pi username and password.

### Option 19 — Setup/repair Remote Access (SSH + XRDP)

Installs and repairs SSH and Remote Desktop (XRDP + Xorg) so you can reach the
Pi from Windows. It runs `scripts/setup_remote_access.sh`, which:

- installs and enables **SSH** (`openssh-server`, `systemctl enable/restart ssh`),
- installs/repairs **XRDP + Xorg** (`xrdp`, `xorgxrdp`, `xserver-xorg-core`,
  `xserver-xorg-input-all`, `xserver-xorg-video-dummy`, `dbus-x11`; tries
  `xserver-xorg-legacy` but does not fail if it is unavailable),
- writes `/etc/X11/Xwrapper.config` (`allowed_users=anybody`,
  `needs_root_rights=yes`),
- enables and restarts XRDP,
- on **Trixie or newer**, sets boot to **Console + require login** (`B1`) to
  avoid the XRDP *"authentication agent already exists"* polkit clash that
  happens when the same user is logged in both locally and over RDP. On
  **Bookworm/Bullseye** autologin is left as-is (simultaneous login works there),
- adds your user to useful groups if present (`video`, `audio`, `plugdev`,
  `gpio`, `i2c`, `spi`),
- prints the hostname and IP and the connection instructions.

Before it runs, the app shows a warning that it installs/repairs SSH, XRDP and
Xorg packages, may require a sudo password and may need a reboot. You must type
exactly **`SETUP_REMOTE`** to proceed — anything else cancels. The script is
**idempotent** (safe to run again). **Reboot is recommended afterwards.**

### Option 20 — Show IP address and remote status

Prints the Pi's **hostname**, **IP address** (`hostname -I`), **SSH status**
(`systemctl is-active ssh`) and **XRDP status** (`systemctl is-active xrdp`),
plus the Remote Desktop connection instructions. Nothing is stored.

### Connecting from Windows

1. Open the **Remote Desktop** app on Windows.
2. **Computer**: the IP address shown by option 19 (or option 18).
3. When the XRDP login page appears, set **Session: `Xorg`**.
4. **Username / Password**: your Raspberry Pi username and password.

> **If XRDP disconnects right after login**, reboot the Pi (`sudo reboot`) and
> try again — the Xorg session needs a clean start after the packages are
> installed/repaired.

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
│   ├── install_boebot.sh              Install all dependencies + camera overlays
│   ├── setup_remote_access.sh         Setup/repair SSH + XRDP (option 18)
│   └── run_boebot_test.sh             Build and run the app
└── src/main/java/my/boebot/
    ├── Main.java                       Entry point and menu loop
    ├── BotConfig.java                  Loads config/boebot.properties
    ├── AppLogger.java                  Saves logs to logs/<hostname>/
    ├── PCA9685.java                    I2C driver for PCA9685 Servo HAT
    ├── RawKey.java                     Single-keypress reader for interactive servo tests
    ├── ServoSafety.java                Ctrl+C-safe servo shutdown (turns outputs off)
    ├── InteractiveWheel.java           Shared incremental wheel control (options 5, 6)
    ├── SystemInfoTest.java             Option 1:  System information
    ├── I2CDetectTest.java              Option 2:  I2C detect / confirm 0x40
    ├── PCA9685InitTest.java            Option 3:  PCA9685 initialization
    ├── CalibrateNeutralTest.java       Option 4:  Calibrate wheel neutral (1500us)
    ├── RightWheelTest.java             Option 5:  Right wheel CH14 (incremental)
    ├── LeftWheelTest.java              Option 6:  Left wheel CH15 (incremental)
    ├── BothWheelsTest.java             Option 7:  Both wheels auto sequence
    ├── DriveControlTest.java           Option 8:  Remote control (8/2/4/6, 5 stop)
    ├── GripperTest.java                Option 9:  MG90S gripper servo CH0
    ├── CameraModule3DetectTest.java    Option 10: Camera Module 3 detection
    ├── CameraModule3Test.java          Option 11: Camera Module 3 still capture
    ├── CameraModule3PreviewTest.java   Option 12: Camera Module 3 live preview
    ├── ToFCameraTest.java              Option 13: ArduCam ToF SDK detection
    ├── ToFCaptureTest.java             Option 14: ArduCam ToF capture/save
    ├── ToFPreviewTest.java             Option 15: ArduCam ToF live preview
    ├── DualCameraViewTest.java         Option 16: Dual camera live view CAM0+CAM1
    ├── FullDriveCameraTest.java        Options 17/18: Full test BOEBOT / SUMOBOT
    ├── RemoteAccessSetupTest.java      Option 19: Setup/repair Remote Access SSH + XRDP
    └── RemoteStatusTest.java           Option 20: Show IP address and remote status
```

---

## Notes

- This app runs **directly on the Raspberry Pi**. No SSH credentials needed inside the app.
- **Do not install Claude Code on the Raspberry Pi.** Use the normal Raspberry Pi terminal.
- BOEBOT has no line sensors — line sensor code is not included.
- No Sumobot code is included.
- Built with Java 17 and Pi4J 2.6.0 (I2C via LinuxFS provider).
