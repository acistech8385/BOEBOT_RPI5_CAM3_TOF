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
echo "[1/9] Updating apt package lists..."
sudo apt-get update -y -qq || true

# ---- Step 2: SSH ----
echo ""
echo "[2/9] Installing and enabling SSH..."
sudo apt-get install -y openssh-server || true
if command -v raspi-config >/dev/null 2>&1; then
    sudo raspi-config nonint do_ssh 0 || true
fi
sudo systemctl enable ssh || true
sudo systemctl restart ssh || true

# ---- Step 3: XRDP + Xorg ----
echo ""
echo "[3/9] Installing/repairing XRDP and Xorg packages..."
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
echo "[4/9] Configuring the X wrapper..."
if [ -d /etc/X11 ]; then
    printf 'allowed_users=anybody\nneeds_root_rights=yes\n' \
        | sudo tee /etc/X11/Xwrapper.config > /dev/null
    echo "  Wrote /etc/X11/Xwrapper.config"
else
    echo "  /etc/X11 not found - skipping Xwrapper.config."
fi

# ---- Step 5: Enable + restart XRDP ----
echo ""
echo "[5/9] Enabling and restarting XRDP..."
sudo systemctl enable xrdp || true
sudo systemctl restart xrdp || true

# ---- Step 6: Fix the classic XRDP "black screen" after login ----
# xrdp's startwm.sh can inherit a stale DBUS_SESSION_BUS_ADDRESS / XDG_RUNTIME_DIR
# from the environment, which makes the desktop session fail to start silently
# (black screen with just a cursor). Unsetting them before the session launches
# is the standard, well-documented fix and lets the SAME user be logged in on the
# physical HDMI screen and over RDP (Session: Xorg) at the same time on Bookworm.
# Idempotent: only patches once, even if this script runs again.
echo ""
echo "[6/9] Fixing the XRDP black-screen issue (startwm.sh)..."
STARTWM=/etc/xrdp/startwm.sh
if [ -f "$STARTWM" ]; then
    if grep -q "unset DBUS_SESSION_BUS_ADDRESS" "$STARTWM"; then
        echo "  Already patched - skipping."
    else
        sudo cp "$STARTWM" "$STARTWM.bak"
        sudo sed -i '/^test -x \/etc\/X11\/Xsession/i unset DBUS_SESSION_BUS_ADDRESS\nunset XDG_RUNTIME_DIR' "$STARTWM"
        echo "  Patched $STARTWM (backup saved as $STARTWM.bak)"
    fi
    sudo systemctl restart xrdp || true
else
    echo "  $STARTWM not found - skipping (xrdp may not be installed)."
fi

# ---- Step 7: Disable autologin on Trixie+ (avoids XRDP polkit session clash) ----
# On Bookworm, simultaneous local + RDP login of the same user works fine (with
# the startwm.sh fix above), so we leave autologin alone. On Trixie (and newer) a
# second same-user session fails with "An authentication agent already exists
# for the given subject", so we set the boot behaviour to Console + require login
# (B1) to avoid the clash. XRDP still provides a full desktop remotely.
echo ""
echo "[7/9] Checking boot behaviour (autologin) for this OS..."
OS_CODENAME=""
if [ -f /etc/os-release ]; then
    OS_CODENAME=$(. /etc/os-release 2>/dev/null; echo "$VERSION_CODENAME")
fi
echo "  OS codename: ${OS_CODENAME:-unknown}"
if [ "$OS_CODENAME" = "bookworm" ] || [ "$OS_CODENAME" = "bullseye" ]; then
    echo "  Bookworm/Bullseye: leaving autologin as-is (simultaneous login works)."
elif command -v raspi-config >/dev/null 2>&1; then
    echo "  Trixie or newer: setting boot to Console + require login (B1)"
    echo "  to avoid the XRDP 'authentication agent already exists' clash."
    sudo raspi-config nonint do_boot_behaviour B1 || true
    echo "  Done. The local screen becomes a console; XRDP still gives a desktop."
else
    echo "  raspi-config not found - skipping. If RDP drops with a polkit error,"
    echo "  disable desktop autologin manually (raspi-config -> System -> Boot)."
fi

# ---- Step 8: Add current user to useful groups (if they exist) ----
CURRENT_USER="$(whoami)"
echo ""
echo "[8/9] Adding user '$CURRENT_USER' to useful Raspberry Pi groups (if present)..."
for grp in video audio plugdev gpio i2c spi; do
    if getent group "$grp" >/dev/null 2>&1; then
        sudo usermod -aG "$grp" "$CURRENT_USER" || true
        echo "  Added to group: $grp"
    fi
done

# ---- Step 9: Show connection info ----
echo ""
echo "[9/9] Remote access information:"
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
