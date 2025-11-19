"""
Audio Level Monitoring Tool
Use this to find the right AUDIO_THRESHOLD value

Instructions:
1. Upload this file to ESP32
2. Run it: import test_audio_levels
3. Make different sounds and watch the values
4. Update AUDIO_THRESHOLD in config.py based on results
"""

print("""
================================================
AUDIO LEVEL MONITOR
================================================

This will show you audio peak values from the
nRF52840 sensors so you can calibrate the
AUDIO_THRESHOLD setting.

What to do:
1. Try clapping at different distances
2. Try blowing on the sensor
3. Try talking, tapping, music, etc.
4. Note the peak values you see

Then update config.py:
  AUDIO_THRESHOLD = <your_value>

Press Ctrl+C to stop
================================================
""")

import time
import config
from ble_scanner import BLEScanner

# Initialize BLE scanner
print("Connecting to sensors...")
ble_scanner = BLEScanner(config.SENSOR_DEVICES)
ble_scanner.start_scan()

# Wait for connections
print("Waiting for sensors to connect...")
for i in range(20):
    time.sleep(0.5)
    status = ble_scanner.get_connection_status()
    connected = sum(1 for v in status.values() if v)
    total = len(status)
    print(f"  BLE: {connected}/{total}", end='\r')
    if connected == total:
        break

print("\n\nSensors connected! Starting audio monitoring...\n")
print("Current threshold: {}".format(config.AUDIO_THRESHOLD))
print("-" * 60)
print("TIME  | SENSOR 1 | SENSOR 2 | STATUS")
print("-" * 60)

max_seen = 0
start_time = time.ticks_ms()

try:
    while True:
        elapsed = (time.ticks_ms() - start_time) // 1000
        
        # Get sensor data
        data1 = ble_scanner.get_sensor_data('sensor_1')
        data2 = ble_scanner.get_sensor_data('sensor_2')
        
        audio1 = data1['audio_peak']
        audio2 = data2['audio_peak']
        
        # Track maximum
        current_max = max(audio1, audio2)
        if current_max > max_seen:
            max_seen = current_max
        
        # Check if either exceeds threshold
        status = ""
        if audio1 > config.AUDIO_THRESHOLD or audio2 > config.AUDIO_THRESHOLD:
            status = "🔥 TRIGGER!"
        elif current_max > config.AUDIO_THRESHOLD * 0.7:
            status = "⚠️  Close..."
        
        # Print line
        print(f"{elapsed:4d}s | {audio1:8d} | {audio2:8d} | {status}")
        
        time.sleep(0.1)  # 10Hz update rate
        
except KeyboardInterrupt:
    print("\n" + "=" * 60)
    print(f"Monitoring stopped.")
    print(f"Maximum audio level seen: {max_seen}")
    print(f"Current threshold: {config.AUDIO_THRESHOLD}")
    print()
    
    if max_seen < config.AUDIO_THRESHOLD:
        print(f"⚠️  RECOMMENDATION: Lower threshold to {int(max_seen * 0.7)}")
    elif max_seen > config.AUDIO_THRESHOLD * 2:
        print(f"⚠️  RECOMMENDATION: Raise threshold to {int(max_seen * 0.5)}")
    else:
        print("✓ Threshold looks good!")
    
    print("=" * 60)

