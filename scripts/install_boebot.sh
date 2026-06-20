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

# ---- Step 2: Java 17 ----
echo ""
echo "[Step 2] Checking Java 17..."

JAVA_OK=false
if command_exists java; then
    # Extract major version from: openjdk version "17.0.x" ...
    JAVA_VER_RAW=$(java -version 2>&1 | head -1)
    JAVA_MAJOR=$(echo "$JAVA_VER_RAW" | grep -oP '(?<=version ")[0-9]+')
    echo "  Found: $JAVA_VER_RAW"
    if [ "$JAVA_MAJOR" = "17" ]; then
        print_ok "Java 17 is already installed."
        JAVA_OK=true
    else
        echo "  Installed Java major version is '$JAVA_MAJOR' (not 17). Will install Java 17."
    fi
fi

if [ "$JAVA_OK" = "false" ]; then
    print_install "openjdk-17-jdk..."
    sudo apt-get install -y openjdk-17-jdk
    if command_exists java; then
        print_ok "Java 17 installed: $(java -version 2>&1 | head -1)"
    else
        print_fail "Java 17 installation may have failed. Check apt output above."
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

# ---- Summary ----
echo ""
echo "======================================================"
echo "  BOEBOT Installation Summary"
echo "======================================================"
echo ""
echo "  Java 17    : $(command_exists java && echo "OK - $(java -version 2>&1 | head -1)" || echo "NOT FOUND")"
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
