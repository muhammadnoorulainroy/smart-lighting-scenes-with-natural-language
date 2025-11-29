"""
ESP32 #1 - BLE Central Main
Connects to nRF52840 sensors and forwards data to ESP32 #2 via UART.
"""

import gc
import time

print("\n" + "=" * 50)
print("  ESP32 #1 - BLE Sensor Hub")
print("=" * 50)
print(f"  Free memory: {gc.mem_free()} bytes")
print("=" * 50 + "\n")

gc.collect()

# Import and run BLE central
try:
    from ble_central import main
    main()
except KeyboardInterrupt:
    print("\n[MAIN] Stopped by user")
except Exception as e:
    print(f"\n[MAIN] Error: {e}")
    import sys
    sys.print_exception(e)
    time.sleep(5)
    import machine
    machine.reset()
