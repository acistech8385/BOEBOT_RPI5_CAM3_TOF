#!/bin/bash
# ============================================================
# BOEBOT RPi5 - Run Hardware Test App
# This script builds and runs the Java hardware test app.
# Runs directly on the Raspberry Pi - no IP/password needed.
# ============================================================

# Find the project root (one level up from scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ""
echo "======================================"
echo "  BOEBOT RPi5 Hardware Test App"
echo "======================================"
echo "Project: $PROJECT_DIR"
echo ""

# Go to project root
cd "$PROJECT_DIR"

# Build the fat JAR with Maven
echo "[BUILD] Running: mvn clean package -q"
mvn clean package -q

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Maven build failed. Check output above."
    exit 1
fi

echo "[BUILD] Build successful."
echo ""

# Find the built JAR file
JAR_FILE="$PROJECT_DIR/target/boebot-hardware-test-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "[ERROR] JAR not found: $JAR_FILE"
    echo "Check target/ folder for the correct JAR name."
    exit 1
fi

# Run the app.
# I2C access on RPi typically needs sudo or the user must be in the i2c group.
# If you see permission errors on /dev/i2c-1, run:
#   sudo usermod -aG i2c $USER   then log out and back in
# Or run this script with sudo.
echo "[RUN] Starting BOEBOT Hardware Test App..."
echo ""
java -jar "$JAR_FILE"
