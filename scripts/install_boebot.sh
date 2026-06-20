#!/bin/bash
# ============================================================
# BOEBOT RPi5 - Installation Script
# Run this on the Raspberry Pi 5 to install all dependencies.
# Safe to run multiple times (idempotent).
# Does NOT require IP address, username, or password.
# ============================================================

set -e  # Stop on first error

echo ""
echo "======================================"
echo "  BOEBOT RPi5 Installation Script"
echo "======================================"
echo ""

# Update package lists
echo "[1/6] Updating package lists..."
sudo apt-get update -y

# Install git
echo "[2/6] Installing git..."
sudo apt-get install -y git

# Install Java 17 JDK
echo "[3/6] Installing OpenJDK 17..."
sudo apt-get install -y openjdk-17-jdk

# Install Maven
echo "[4/6] Installing Maven..."
sudo apt-get install -y maven

# Install I2C tools
echo "[5/6] Installing i2c-tools..."
sudo apt-get install -y i2c-tools

# Install rpicam-apps (for Camera Module 3)
echo "[6/6] Installing rpicam-apps (Camera Module 3 support)..."
if sudo apt-get install -y rpicam-apps 2>/dev/null; then
    echo "  rpicam-apps installed successfully."
else
    echo "  rpicam-apps not found in package list - may already be installed or use 'libcamera-apps'."
    sudo apt-get install -y libcamera-apps 2>/dev/null || echo "  Skipping - install manually if needed."
fi

# Enable I2C interface using raspi-config nonint
echo ""
echo "[I2C] Enabling I2C interface..."
if command -v raspi-config &> /dev/null; then
    sudo raspi-config nonint do_i2c 0
    echo "  I2C enabled via raspi-config."
else
    echo "  raspi-config not found - enable I2C manually via: sudo raspi-config"
fi

# Verify Java version
echo ""
echo "[CHECK] Java version:"
java -version

# Verify Maven version
echo ""
echo "[CHECK] Maven version:"
mvn -version

# Verify i2cdetect is available
echo ""
echo "[CHECK] i2cdetect version:"
i2cdetect -V 2>&1 || true

echo ""
echo "======================================"
echo "  Installation Complete!"
echo "======================================"
echo ""
echo "Next steps:"
echo ""
echo "  1. Reboot to apply I2C changes (if not already enabled):"
echo "     sudo reboot"
echo ""
echo "  2. After reboot, verify I2C sees the PCA9685 Servo HAT:"
echo "     sudo i2cdetect -y 1"
echo "     (You should see '40' in the grid for address 0x40)"
echo ""
echo "  3. Build and run the hardware test app:"
echo "     chmod +x scripts/run_boebot_test.sh"
echo "     ./scripts/run_boebot_test.sh"
echo ""
echo "Note: This app runs DIRECTLY on the Raspberry Pi."
echo "No IP address, username, or password is needed inside the app."
echo ""
