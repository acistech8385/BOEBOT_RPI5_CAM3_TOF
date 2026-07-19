#!/bin/bash
# ============================================================
# BOEBOT RPi5 - Installation Script
# Idempotent: safe to run multiple times.
# Runs directly on the Raspberry Pi - no IP/password needed.
# ============================================================

REBOOT_NEEDED=false

# ---- Helper functions ----
print_ok()      { echo "  [OK]       $1"; }
print_install() { echo "  [INSTALL]  $1"; }
print_skip()    { echo "  [SKIP]     $1"; }
print_warn()    { echo "  [WARNING]  $1"; }
print_fail()    { echo "  [FAIL]     $1"; }

package_installed() {
    dpkg -l "$1" 2>/dev/null | grep -q "^ii"
}

command_exists() {
    command -v "$1" &>/dev/null
}

# ============================================================
echo ""
echo "======================================================"
echo "  BOEBOT RPi5 Installation Script"
echo "  Raspberry Pi OS / Debian - Idempotent Install"
echo "======================================================"
echo ""

# ---- Step 1: apt update ----
echo "[Step 1] Updating apt package lists..."
sudo apt-get update -y -qq
echo "  Done."

# ---- Step 2: Java JDK (17 or newer) ----
echo ""
echo "[Step 2] Checking Java JDK..."

# The app targets Java 17, but a newer JDK (e.g. 21 on Debian Trixie) builds it
# fine. We need a JDK (javac), not just a JRE - Maven cannot compile on a JRE.
JAVA_OK=false
if command_exists javac; then
    JAVAC_VER=$(javac -version 2>&1 | head -1)
    JAVAC_MAJOR=$(echo "$JAVAC_VER" | grep -oP '[0-9]+' | head -1)
    echo "  Found: $JAVAC_VER"
    if [ -n "$JAVAC_MAJOR" ] && [ "$JAVAC_MAJOR" -ge 17 ]; then
        print_ok "JDK $JAVAC_MAJOR is available (>= 17). OK to build."
        JAVA_OK=true
    else
        echo "  javac major version '$JAVAC_MAJOR' is < 17. Will install a newer JDK."
    fi
fi

if [ "$JAVA_OK" = "false" ]; then
    # Try 17 (Bookworm), then 21 (Trixie), then default-jdk.
    print_install "a Java JDK (openjdk-17-jdk -> openjdk-21-jdk -> default-jdk)..."
    if sudo apt-get install -y openjdk-17-jdk 2>/dev/null; then
        print_ok "openjdk-17-jdk installed."
    elif sudo apt-get install -y openjdk-21-jdk 2>/dev/null; then
        print_ok "openjdk-21-jdk installed."
    elif sudo apt-get install -y default-jdk 2>/dev/null; then
        print_ok "default-jdk installed."
    else
        print_fail "Could not install a JDK. Try: sudo apt-get install -y default-jdk"
    fi
    if command_exists javac; then
        print_ok "JDK ready: $(javac -version 2>&1 | head -1)"
    else
        print_fail "javac still not found - the build will fail without a JDK."
    fi
fi

# ---- Step 3: Maven ----
echo ""
echo "[Step 3] Checking Maven..."

if command_exists mvn; then
    MVN_VER=$(mvn -version 2>&1 | head -1)
    print_ok "Maven is already installed: $MVN_VER"
else
    print_install "maven..."
    sudo apt-get install -y maven
    if command_exists mvn; then
        print_ok "Maven installed: $(mvn -version 2>&1 | head -1)"
    else
        print_fail "Maven installation may have failed."
    fi
fi

# ---- Step 4: i2c-tools ----
echo ""
echo "[Step 4] Checking i2c-tools..."

if command_exists i2cdetect; then
    print_ok "i2c-tools already installed."
else
    print_install "i2c-tools..."
    sudo apt-get install -y i2c-tools
    if command_exists i2cdetect; then
        print_ok "i2c-tools installed."
    fi
fi

# ---- Step 5: Camera tools (rpicam-apps or libcamera-apps) ----
echo ""
echo "[Step 5] Checking Raspberry Pi camera tools..."

CAM_FOUND=false
if command_exists rpicam-still; then
    print_ok "rpicam-apps already installed (rpicam-still found)."
    CAM_FOUND=true
elif command_exists libcamera-still; then
    print_ok "libcamera-apps already installed (libcamera-still found)."
    CAM_FOUND=true
fi

if [ "$CAM_FOUND" = "false" ]; then
    print_install "Camera tools..."
    echo "  Trying rpicam-apps (Raspberry Pi OS Bookworm)..."
    if sudo apt-get install -y rpicam-apps 2>/dev/null; then
        print_ok "rpicam-apps installed."
        CAM_FOUND=true
    else
        echo "  rpicam-apps not found. Trying libcamera-apps..."
        if sudo apt-get install -y libcamera-apps 2>/dev/null; then
            print_ok "libcamera-apps installed."
            CAM_FOUND=true
        fi
    fi
    if [ "$CAM_FOUND" = "false" ]; then
        print_warn "Could not install camera tools via apt."
        echo ""
        echo "  To install manually:"
        echo "    sudo apt-get update"
        echo "    sudo apt-get install -y rpicam-apps"
        echo "  Or check: https://www.raspberrypi.com/documentation/computers/camera_software.html"
    fi
fi

# ---- Step 6: Python3 (needed for ArduCam ToF SDK examples) ----
echo ""
echo "[Step 6] Checking Python 3..."

if command_exists python3; then
    print_ok "Python 3 is already installed: $(python3 --version)"
else
    print_install "python3..."
    sudo apt-get install -y python3 python3-pip
    print_ok "Python 3 installed."
fi

# ---- Step 7: Enable I2C ----
echo ""
echo "[Step 7] Checking I2C interface..."

if [ -e /dev/i2c-1 ]; then
    print_ok "I2C is active (/dev/i2c-1 exists). No reboot needed."
else
    echo "  /dev/i2c-1 does not exist. I2C is not yet active."

    # Try to enable via raspi-config
    if command_exists raspi-config; then
        echo "  Enabling I2C via raspi-config..."
        sudo raspi-config nonint do_i2c 0
        echo ""
        echo "  *** I2C has been enabled. ***"
        echo "  *** REBOOT REQUIRED to activate /dev/i2c-1. ***"
        REBOOT_NEEDED=true
    else
        # Try manually adding to /boot/config.txt or /boot/firmware/config.txt
        CONFIG_FILE=""
        if [ -f /boot/firmware/config.txt ]; then
            CONFIG_FILE="/boot/firmware/config.txt"
        elif [ -f /boot/config.txt ]; then
            CONFIG_FILE="/boot/config.txt"
        fi

        if [ -n "$CONFIG_FILE" ]; then
            if grep -q "dtparam=i2c_arm=on" "$CONFIG_FILE"; then
                print_ok "I2C already in $CONFIG_FILE but /dev/i2c-1 missing. Try rebooting."
                REBOOT_NEEDED=true
            else
                echo "  Adding dtparam=i2c_arm=on to $CONFIG_FILE..."
                echo "dtparam=i2c_arm=on" | sudo tee -a "$CONFIG_FILE" > /dev/null
                echo "  *** REBOOT REQUIRED to activate I2C. ***"
                REBOOT_NEEDED=true
            fi
        else
            print_warn "Could not find config.txt. Enable I2C manually:"
            echo "    sudo raspi-config -> Interface Options -> I2C -> Enable"
            REBOOT_NEEDED=true
        fi
    fi
fi

# ---- Step 7b: Camera overlays (Camera Module 3 on CAM0 + ArduCam ToF on CAM1) ----
echo ""
echo "[Step 7b] Configuring cameras (Camera Module 3 -> CAM0, ArduCam ToF -> CAM1)..."

CAM_CONFIG=""
if [ -f /boot/firmware/config.txt ]; then
    CAM_CONFIG="/boot/firmware/config.txt"
elif [ -f /boot/config.txt ]; then
    CAM_CONFIG="/boot/config.txt"
fi

if [ -z "$CAM_CONFIG" ]; then
    print_warn "Could not find config.txt. Set camera overlays manually:"
    echo "    camera_auto_detect=0"
    echo "    dtoverlay=imx708,cam0"
    echo "    dtoverlay=arducam-pivariety,cam1"
else
    CAM_CHANGED=false

    # camera_auto_detect MUST be 0 - auto-detect breaks the ArduCam ToF I2C bus.
    if grep -q "^camera_auto_detect=1" "$CAM_CONFIG"; then
        sudo sed -i 's/^camera_auto_detect=1/camera_auto_detect=0/' "$CAM_CONFIG"
        echo "  Set camera_auto_detect=0"
        CAM_CHANGED=true
    elif ! grep -q "^camera_auto_detect=0" "$CAM_CONFIG"; then
        echo "camera_auto_detect=0" | sudo tee -a "$CAM_CONFIG" > /dev/null
        echo "  Added camera_auto_detect=0"
        CAM_CHANGED=true
    fi

    # Camera Module 3 (imx708) on CAM0
    if ! grep -q "^dtoverlay=imx708,cam0" "$CAM_CONFIG"; then
        echo "dtoverlay=imx708,cam0" | sudo tee -a "$CAM_CONFIG" > /dev/null
        echo "  Added dtoverlay=imx708,cam0"
        CAM_CHANGED=true
    fi

    # ArduCam ToF on CAM1
    if ! grep -q "^dtoverlay=arducam-pivariety,cam1" "$CAM_CONFIG"; then
        echo "dtoverlay=arducam-pivariety,cam1" | sudo tee -a "$CAM_CONFIG" > /dev/null
        echo "  Added dtoverlay=arducam-pivariety,cam1"
        CAM_CHANGED=true
    fi

    if [ "$CAM_CHANGED" = "true" ]; then
        print_ok "Camera overlays written to $CAM_CONFIG."
        echo "  *** REBOOT REQUIRED to activate the cameras. ***"
        REBOOT_NEEDED=true
    else
        print_ok "Camera overlays already set in $CAM_CONFIG."
    fi
fi

# ---- Helper: pip install ArducamDepthCamera + dependencies ----
pip_install_arducam() {
    echo "  Installing: ArducamDepthCamera opencv-python numpy<2.0.0 via pip3..."
    # Try plain pip3 first; if blocked by externally-managed-environment, use --break-system-packages
    if pip3 install ArducamDepthCamera opencv-python "numpy<2.0.0" --quiet 2>/dev/null; then
        print_ok "ArducamDepthCamera installed via pip3."
        return 0
    elif pip3 install --break-system-packages ArducamDepthCamera opencv-python "numpy<2.0.0" --quiet 2>/dev/null; then
        print_ok "ArducamDepthCamera installed via pip3 (--break-system-packages)."
        return 0
    else
        print_warn "pip3 install failed. Trying with sudo..."
        if sudo pip3 install --break-system-packages ArducamDepthCamera opencv-python "numpy<2.0.0" --quiet 2>/dev/null; then
            print_ok "ArducamDepthCamera installed via sudo pip3."
            return 0
        fi
    fi
    print_warn "pip3 install failed. Try manually:"
    echo "    pip3 install --break-system-packages ArducamDepthCamera opencv-python \"numpy<2.0.0\""
    return 1
}

# ---- Helper: run ArduCam install script (tries both known filenames) ----
run_arducam_install() {
    local tof_dir="$1"
    local script=""
    # Try correct name first, then fallback for older SDK versions
    if   [ -f "$tof_dir/Install_dependencies.sh" ];          then script="$tof_dir/Install_dependencies.sh"
    elif [ -f "$tof_dir/Install_dependencies_raspbian.sh" ];  then script="$tof_dir/Install_dependencies_raspbian.sh"
    fi

    if [ -n "$script" ]; then
        echo "  Running: bash $script"
        echo "  (Answering 'n' to reboot prompt — will not reboot mid-install.)"
        echo ""
        # Pass 'n' to auto-answer the 'reboot now? (y/n)' prompt
        echo "n" | bash "$script" 2>&1 | grep -v "^$" || true
        echo ""
        # Always run pip install after the script (recommended by ArduCam installer)
        pip_install_arducam
    else
        print_warn "No ArduCam install script found in $tof_dir"
        echo "  Trying pip install directly..."
        pip_install_arducam
    fi
}

# ---- Step 8: ArduCam ToF SDK ----
echo ""
echo "[Step 8] Checking ArduCam ToF SDK..."

TOF_DIR="$HOME/Arducam_tof_camera"

if [ -d "$TOF_DIR" ]; then
    print_ok "ArduCam ToF SDK already exists: $TOF_DIR"
else
    echo "  ArduCam ToF SDK not found at $TOF_DIR"
    echo "  Cloning from GitHub..."
    echo ""

    if git clone https://github.com/ArduCAM/Arducam_tof_camera.git "$TOF_DIR"; then
        print_ok "ArduCam ToF SDK cloned to $TOF_DIR"
        echo ""
        run_arducam_install "$TOF_DIR"
    else
        print_warn "Could not clone ArduCam ToF SDK."
        echo ""
        echo "  Check your internet connection, then try manually:"
        echo "    git clone https://github.com/ArduCAM/Arducam_tof_camera.git ~/Arducam_tof_camera"
        echo ""
        echo "  This does NOT prevent the rest of BOEBOT from working."
    fi
fi

# ---- Step 9: ArduCam ToF Python dependencies ----
echo ""
echo "[Step 9] Checking ArduCam ToF Python dependencies..."

# numpy
if python3 -c "import numpy" 2>/dev/null; then
    print_ok "numpy is available."
else
    print_install "numpy (needed for ToF capture and preview)..."
    pip3 install numpy --quiet 2>/dev/null \
        || sudo pip3 install numpy --quiet 2>/dev/null \
        || sudo apt-get install -y python3-numpy -qq 2>/dev/null \
        || true
    if python3 -c "import numpy" 2>/dev/null; then
        print_ok "numpy installed."
    else
        print_warn "numpy install may have failed. Try: pip3 install numpy"
    fi
fi

# opencv-python (cv2) - needed for PNG image output from ToF
if python3 -c "import cv2" 2>/dev/null; then
    print_ok "opencv-python (cv2) is available."
else
    print_install "opencv-python (needed for ToF PNG output)..."
    sudo apt-get install -y python3-opencv -qq 2>/dev/null \
        || pip3 install opencv-python --quiet 2>/dev/null \
        || sudo pip3 install opencv-python --quiet 2>/dev/null \
        || true
    if python3 -c "import cv2" 2>/dev/null; then
        print_ok "opencv-python installed."
    else
        print_warn "cv2 not available. ToF depth PNG output will be skipped."
        echo "  Try: sudo apt-get install -y python3-opencv"
    fi
fi

# picamera2 - needed for the RGB feed in the dual camera view (option 14)
if python3 -c "import picamera2" 2>/dev/null; then
    print_ok "picamera2 is available. Dual camera view is ready."
else
    print_install "python3-picamera2 (needed for option 14 dual camera view)..."
    sudo apt-get install -y python3-picamera2 -qq 2>/dev/null || true
    if python3 -c "import picamera2" 2>/dev/null; then
        print_ok "python3-picamera2 installed."
    else
        print_warn "picamera2 not available. Option 14 dual camera view will not work."
        echo "  Try: sudo apt-get install -y python3-picamera2"
    fi
fi

# ArducamDepthCamera module — auto-install if missing
if python3 -c "import ArducamDepthCamera" 2>/dev/null; then
    print_ok "ArducamDepthCamera SDK is importable. ToF tests are ready."
else
    print_warn "ArducamDepthCamera module not importable. Running ArduCam installer..."
    echo ""
    if [ -d "$TOF_DIR" ]; then
        run_arducam_install "$TOF_DIR"
        echo ""
        # Verify after install
        if python3 -c "import ArducamDepthCamera" 2>/dev/null; then
            print_ok "ArducamDepthCamera now importable. ToF tests are ready."
        else
            print_warn "ArducamDepthCamera still not importable after install."
            echo "  Try rebooting and running this script again: sudo reboot"
        fi
    else
        print_warn "SDK folder missing ($TOF_DIR). Run this script again to clone it."
    fi
fi

# ---- Step 8b: Re-normalise camera overlays (MUST run after the ToF SDK install) ----
# The ArduCam ToF installer adds its own dtoverlay=arducam-pivariety / ,cam0
# lines which CONFLICT with imx708,cam0 and break Camera Module 3. Re-write the
# one known-good set so both cameras work: CM3 on CAM0, ArduCam ToF on CAM1.
echo ""
echo "[Step 8b] Re-normalising camera overlays (CM3 -> CAM0, ToF -> CAM1)..."

NORM_CONFIG=""
if [ -f /boot/firmware/config.txt ]; then
    NORM_CONFIG="/boot/firmware/config.txt"
elif [ -f /boot/config.txt ]; then
    NORM_CONFIG="/boot/config.txt"
fi

if [ -z "$NORM_CONFIG" ]; then
    print_warn "config.txt not found - set camera overlays manually."
else
    # Strip every camera_auto_detect / imx708 / arducam-pivariety line we manage,
    # then append exactly one correct set (idempotent).
    sudo sed -i '/^camera_auto_detect=/d' "$NORM_CONFIG"
    sudo sed -i '/^dtoverlay=imx708/d' "$NORM_CONFIG"
    sudo sed -i '/^dtoverlay=arducam-pivariety/d' "$NORM_CONFIG"
    {
        echo "camera_auto_detect=0"
        echo "dtoverlay=imx708,cam0"
        echo "dtoverlay=arducam-pivariety,cam1"
    } | sudo tee -a "$NORM_CONFIG" > /dev/null
    print_ok "Camera overlays normalised in $NORM_CONFIG:"
    grep -E "^camera_auto_detect|^dtoverlay=imx708|^dtoverlay=arducam" "$NORM_CONFIG" | sed 's/^/    /'
    echo "  *** REBOOT REQUIRED for the cameras. ***"
    REBOOT_NEEDED=true
fi

# ---- Summary ----
echo ""
echo "======================================================"
echo "  BOEBOT Installation Summary"
echo "======================================================"
echo ""
echo "  Java JDK    : $(command_exists javac && echo "OK - $(javac -version 2>&1 | head -1)" || echo "NO JDK (build will fail)")"
echo "  Maven      : $(command_exists mvn && echo "OK" || echo "NOT FOUND")"
echo "  i2c-tools  : $(command_exists i2cdetect && echo "OK" || echo "NOT FOUND")"
echo "  rpicam-still: $(command_exists rpicam-still && echo "OK" || echo "not found")"
echo "  libcam-still: $(command_exists libcamera-still && echo "OK" || echo "not found")"
echo "  Python 3   : $(command_exists python3 && echo "OK - $(python3 --version)" || echo "NOT FOUND")"
echo "  numpy      : $(python3 -c "import numpy; print('OK - ' + numpy.__version__)" 2>/dev/null || echo "NOT FOUND")"
echo "  cv2        : $(python3 -c "import cv2; print('OK')" 2>/dev/null || echo "not found")"
echo "  ArducamSDK : $(python3 -c "import ArducamDepthCamera; print('OK')" 2>/dev/null || echo "NOT IMPORTABLE")"
echo "  I2C device : $([ -e /dev/i2c-1 ] && echo "OK - /dev/i2c-1 exists" || echo "NOT ACTIVE (reboot needed?)")"
echo "  ArduCam SDK: $([ -d "$HOME/Arducam_tof_camera" ] && echo "OK - $HOME/Arducam_tof_camera" || echo "NOT INSTALLED")"
echo ""

if [ "$REBOOT_NEEDED" = "true" ]; then
    echo "======================================================"
    echo "  *** REBOOT REQUIRED ***"
    echo "======================================================"
    echo ""
    echo "  I2C was just enabled. You must reboot before I2C will work."
    echo ""
    echo "  Run:"
    echo "    sudo reboot"
    echo ""
    echo "  After reboot, verify I2C:"
    echo "    sudo i2cdetect -y 1"
    echo "  (Look for '40' in the grid = PCA9685 Servo HAT detected)"
    echo ""
    echo "  Then run the BOEBOT test app:"
    echo "    cd ~/BOEBOT_RPI5_CAM3_TOF/BOEBOT_RPI5_CAM3_TOF"
    echo "    ./scripts/run_boebot_test.sh"
else
    echo "======================================================"
    echo "  Next Steps"
    echo "======================================================"
    echo ""
    echo "  1. Verify I2C sees the PCA9685 Servo HAT:"
    echo "     sudo i2cdetect -y 1"
    echo "     (Look for '40' in the output grid)"
    echo ""
    echo "  2. Run the BOEBOT Hardware Test App:"
    echo "     ./scripts/run_boebot_test.sh"
fi
echo ""
