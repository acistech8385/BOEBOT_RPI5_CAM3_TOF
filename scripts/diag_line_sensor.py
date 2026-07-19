#!/usr/bin/env python3
# Standalone line-sensor diagnostic - NOT part of the app menu.
# Run directly:  python3 scripts/diag_line_sensor.py 20
# (20 = GPIO number to test, change to whichever pin you're probing)
import sys, time

try:
    import lgpio
except ImportError:
    print("FAIL: python3-lgpio not installed"); sys.exit(1)

if len(sys.argv) < 2:
    print("Usage: python3 diag_line_sensor.py <GPIO_NUMBER>")
    sys.exit(1)

pin = int(sys.argv[1])

h = None
for chip in (4, 0, 1):
    try:
        h = lgpio.gpiochip_open(chip)
        print(f"Opened gpiochip{chip}")
        break
    except Exception as e:
        print(f"gpiochip{chip} failed: {e}")
if h is None:
    print("FAIL: could not open any gpiochip"); sys.exit(1)

PULL_NONE = getattr(lgpio, "SET_PULL_NONE", 0)
print(f"PULL_NONE flag value = {PULL_NONE}")

# --- Test A: raw read with NO charge step, input only ---
print("\n=== Test A: claim as INPUT directly (no charge), read raw level 5x ===")
try:
    lgpio.gpio_claim_input(h, pin, PULL_NONE)
    for i in range(5):
        v = lgpio.gpio_read(h, pin)
        print(f"  raw level = {v}")
        time.sleep(0.2)
    lgpio.gpio_free(h, pin)
except Exception as e:
    print(f"  Test A error: {e}")

# --- Test B: charge HIGH, then watch it live for up to 5 seconds ---
print("\n=== Test B: charge 1ms HIGH, then watch level every 200ms for up to 5s ===")
try:
    lgpio.gpio_claim_output(h, pin, 1)
    time.sleep(0.001)
    lgpio.gpio_free(h, pin)
    lgpio.gpio_claim_input(h, pin, PULL_NONE)
    t0 = time.perf_counter()
    went_low_at = None
    while (time.perf_counter() - t0) < 5.0:
        v = lgpio.gpio_read(h, pin)
        elapsed_ms = (time.perf_counter() - t0) * 1000
        print(f"  t={elapsed_ms:7.1f} ms  level={v}")
        if v == 0:
            went_low_at = elapsed_ms
            break
        time.sleep(0.2)
    lgpio.gpio_free(h, pin)
    if went_low_at is not None:
        print(f"\n  RESULT: pin went LOW after {went_low_at:.1f} ms")
    else:
        print("\n  RESULT: pin NEVER went low within 5 full seconds")
except Exception as e:
    print(f"  Test B error: {e}")

lgpio.gpiochip_close(h)
print("\nDone.")
