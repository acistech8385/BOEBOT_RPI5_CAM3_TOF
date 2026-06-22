#!/bin/bash
# ============================================================
# BOEBOT RPi5 - Remote Access Setup / Repair (SSH + XRDP)
# Idempotent: safe to run multiple times.
#
# This script does NOT ask for, store, or hardcode any IP address,
# username, or password. It only installs/enables services and prints
# the Pi's own hostname/IP so you can connect with Remote Desktop.
# ============================================================

echo ""
echo "======================================================"
echo "  BOEBOT Remote Access Setup (SSH + XRDP)"
echo "======================================================"
echo ""

# ---- Step 1: apt update ----
echo "[1/7] Updating apt package lists..."
sudo apt-get update -y -qq || true

# ---- Step 2: SSH ----
echo ""
echo "[2/7] Installing and enabling SSH..."
sudo apt-get install -y openssh-server || true
if command -v raspi-config >/dev/null 2>&1; then
    sudo raspi-config nonint do_ssh 0 || true
fi
sudo systemctl enable ssh || true
sudo systemctl restart ssh || true

# ---- Step 3: XRDP + Xorg ----
echo ""
echo "[3/7] Installing/repairing XRDP and Xorg packages..."
sudo apt-get install -y \
    xrdp \
    xorgxrdp \
    xserver-xorg-core \
    xserver-xorg-input-all \
    xserver-xorg-video-dummy \
    dbus-x11 || true

# Optional legacy X wrapper - do not fail if it is unavailable.
if sudo apt-get install -y xserver-xorg-legacy 2>/dev/null; then
    echo "  Installed xserver-xorg-legacy."
else
    echo "  xserver-xorg-legacy not available - skipping (not required)."
fi

# ---- Step 4: Xwrapper.config ----
echo ""
echo "[4/7] Configuring the X wrapper..."
if [ -d /etc/X11 ]; then
    printf 'allowed_users=anybody\nneeds_root_rights=yes\n' \
        | sudo tee /etc/X11/Xwrapper.config > /dev/null
    echo "  Wrote /etc/X11/Xwrapper.config"
else
    echo "  /etc/X11 not found - skipping Xwrapper.config."
fi

# ---- Step 5: Enable + restart XRDP ----
echo ""
echo "[5/7] Enabling and restarting XRDP..."
sudo systemctl enable xrdp || true
sudo systemctl restart xrdp || true

# ---- Step 6: Add current user to useful groups (if they exist) ----
CURRENT_USER="$(whoami)"
echo ""
echo "[6/7] Adding user '$CURRENT_USER' to useful Raspberry Pi groups (if present)..."
for grp in video audio plugdev gpio i2c spi; do
    if getent group "$grp" >/dev/null 2>&1; then
        sudo usermod -aG "$grp" "$CURRENT_USER" || true
        echo "  Added to group: $grp"
    fi
done

# ---- Step 7: Show connection info ----
echo ""
echo "[7/7] Remote access information:"
echo "  Hostname : $(hostname)"
echo "  IP       : $(hostname -I)"
echo ""
echo "  --- Connect with the Windows Remote Desktop app ---"
echo "  Computer : use the IP address shown above."
echo "  Session  : Xorg"
echo "  Username/password: use your Raspberry Pi username and password."
echo ""
echo "  Reboot is recommended after remote setup."
echo ""
