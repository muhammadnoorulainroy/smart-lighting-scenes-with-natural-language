# config.py - ESP32 #1 (BLE Central)
"""
Configuration for BLE Central ESP32.
Connects to nRF52840 sensors and receives data via notifications.
"""

# ============================================
# BLE CONNECTION MODE
# ============================================
# This ESP32 operates as a BLE Central:
# 1. Scans ONCE to discover sensors (initial discovery only)
# 2. Connects to discovered sensors
# 3. Subscribes to GATT notifications
# 4. Receives real-time data push from sensors (event-driven)
#
# Data flow: Sensor value changes -> BLE notify -> ESP32 #1 -> UART -> ESP32 #2
# Latency: Near-instant (notification-based, no polling)

BLE_TARGET_PREFIX = "SmartLight-Sensor"  # Prefix for sensor names
BLE_RSSI_THRESHOLD = -95  # Minimum signal strength (dBm)

# Discovery scan settings (used only when searching for disconnected sensors)
BLE_DISCOVERY_SCAN_MS = 5000  # Duration of discovery scan
BLE_RECONNECT_DELAY_S = 5     # Wait time before rescanning for lost sensors

# ============================================
# DEBUG
# ============================================
DEBUG = True

