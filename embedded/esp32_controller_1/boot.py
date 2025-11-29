"""
ESP32 #1 Boot - BLE Sensor Hub
Minimal boot for BLE central operations.
"""

import gc
import esp

# Disable debug output for cleaner logs
esp.osdebug(None)

# Enable garbage collection
gc.enable()
gc.threshold(gc.mem_free() // 4 + gc.mem_alloc())
gc.collect()

print("[BOOT] ESP32 #1 - BLE Sensor Hub")
print(f"[BOOT] Free memory: {gc.mem_free()} bytes")
