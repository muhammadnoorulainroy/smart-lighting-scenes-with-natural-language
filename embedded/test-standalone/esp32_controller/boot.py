"""
Boot script for ESP32
Runs before main.py
"""

import gc
import esp

# Disable debug output
esp.osdebug(None)

# Enable garbage collection
gc.enable()
gc.collect()

print("Boot complete. Starting main.py...")


