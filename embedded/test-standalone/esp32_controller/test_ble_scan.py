"""
Quick BLE scan test - run this independently to debug BLE
"""

import bluetooth
from micropython import const
import time

_IRQ_SCAN_RESULT = const(5)
_IRQ_SCAN_DONE = const(6)

devices_found = []

def ble_irq(event, data):
    global devices_found
    if event == _IRQ_SCAN_RESULT:
        addr_type, addr, adv_type, rssi, adv_data = data
        addr_str = ':'.join(['%02X' % b for b in addr])
        
        # Try to decode name
        name = decode_name(adv_data)
        
        device_info = {
            'addr': addr_str,
            'name': name,
            'rssi': rssi,
            'adv_type': adv_type
        }
        
        devices_found.append(device_info)
        print(f"Found: {name or 'Unknown'} | {addr_str} | RSSI: {rssi}")
        
    elif event == _IRQ_SCAN_DONE:
        print("\n--- Scan Complete ---")
        print(f"Total devices found: {len(devices_found)}")
        print("\nLooking for:")
        print("  - SmartLight-Sensor-1")
        print("  - SmartLight-Sensor-2")
        print("\nDevices with names:")
        for dev in devices_found:
            if dev['name']:
                match1 = "✓ MATCH!" if 'SmartLight-Sensor-1' in dev['name'] else ""
                match2 = "✓ MATCH!" if 'SmartLight-Sensor-2' in dev['name'] else ""
                print(f"  {dev['name']} {match1}{match2}")

def decode_name(adv_data):
    """Decode device name from advertisement data"""
    try:
        i = 0
        while i < len(adv_data):
            length = adv_data[i]
            if length == 0:
                break
            typ = adv_data[i + 1]
            # Type 0x09 is complete local name, 0x08 is shortened
            if typ == 0x09 or typ == 0x08:
                return bytes(adv_data[i + 2:i + 1 + length]).decode('utf-8')
            i += 1 + length
    except:
        pass
    return None

# Main test
print("=" * 50)
print("BLE SCAN TEST")
print("=" * 50)
print("\nInitializing BLE...")

ble = bluetooth.BLE()
ble.active(True)
ble.irq(ble_irq)

print("BLE Active:", ble.active())
print("\nStarting 10-second scan...")
print("(Make sure nRF52840 sensors are powered on!)")
print("-" * 50)

devices_found = []
# Active scan (True parameter) to get device names from scan response
ble.gap_scan(10000, 30000, 30000, True)

# Wait for scan to complete
time.sleep(11)

print("\n" + "=" * 50)
print("Test complete!")
print("=" * 50)

