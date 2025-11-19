"""
ESP32 Smart Lighting Controller - Main Application
Standalone test version (no MQTT backend)
"""

import time
import gc
from machine import reset

import config
from led_controller import LEDController
from ble_scanner import BLEScanner
from oled_display import OLEDDisplay
from sensor_logic import SensorLogic

# ============================================
# GLOBAL STATE
# ============================================

# Controllers
led_controller = None
ble_scanner = None
oled_display = None
sensor_logic = None

# System state
global_mode = config.GLOBAL_MODE
lights_on = True
current_page = 0  # OLED page (0=overview, 1=room1, 2=room2, 3=status)
last_scan_time = 0
last_oled_update = 0
start_time = time.ticks_ms()

# LED states (room configurations)
led_states = []

def init_system():
    """Initialize all system components"""
    global led_controller, ble_scanner, oled_display, sensor_logic, led_states
    
    print("=" * 40)
    print("Smart Lighting Controller v1.0")
    print("Standalone Test Mode")
    print("=" * 40)
    
    # Initialize LED controller
    print("\n[1/4] Initializing LEDs...")
    led_controller = LEDController(config.LED_PIN, config.NUM_LEDS)
    led_controller.test_pattern()
    print(f"  → {config.NUM_LEDS} LEDs on GPIO {config.LED_PIN}")
    
    # Initialize OLED
    print("\n[2/4] Initializing OLED...")
    oled_display = OLEDDisplay(
        config.OLED_SDA,
        config.OLED_SCL,
        config.OLED_WIDTH,
        config.OLED_HEIGHT,
        config.OLED_ADDR
    )
    print(f"  → {config.OLED_WIDTH}x{config.OLED_HEIGHT} at 0x{config.OLED_ADDR:02X}")
    
    # Initialize BLE scanner
    print("\n[3/4] Initializing BLE...")
    ble_scanner = BLEScanner(config.SENSOR_DEVICES)
    print(f"  → Scanning for {len(config.SENSOR_DEVICES)} sensors")
    
    # Initialize sensor logic
    print("\n[4/4] Initializing sensor logic...")
    sensor_logic = SensorLogic(config)
    print("  → Sensor effects configured")
    
    # Initialize LED states
    for i in range(config.NUM_LEDS):
        led_states.append({
            'rgb': config.DEFAULT_COLORS[i],
            'brightness': config.DEFAULT_BRIGHTNESS,
            'on': True,
            'has_sensor': False,
            'room_name': f'Room{i+1}'
        })
    
    # Configure rooms with sensors
    for key, sensor_config in config.SENSOR_DEVICES.items():
        led_idx = sensor_config['led_index']
        led_states[led_idx]['has_sensor'] = True
        led_states[led_idx]['room_name'] = sensor_config['room_name']
    
    # Configure static rooms
    for room in config.STATIC_ROOMS:
        led_idx = room['led_index']
        led_states[led_idx]['rgb'] = room['color']
        led_states[led_idx]['brightness'] = room['brightness']
        led_states[led_idx]['room_name'] = room['room_name']
    
    print("\n" + "=" * 40)
    print("System Ready!")
    print("=" * 40)
    print("\nRoom Configuration:")
    for i, state in enumerate(led_states):
        sensor_str = " (SENSOR)" if state['has_sensor'] else ""
        print(f"  LED {i}: {state['room_name']}{sensor_str}")
    print("\nMode:", global_mode.upper())
    print("=" * 40 + "\n")
    
    # Start BLE scan
    ble_scanner.start_scan()

def update_leds():
    """Update LED states based on sensor data and mode"""
    global led_states, global_mode, lights_on
    
    if not lights_on:
        # All LEDs off
        for i in range(config.NUM_LEDS):
            led_controller.set_led(i, (0, 0, 0), 0, False)
        led_controller.update()
        return
    
    # Update each LED
    for i in range(config.NUM_LEDS):
        state = led_states[i]
        base_rgb = state['rgb']
        base_brightness = state['brightness']
        
        # Check if this LED has a sensor
        if state['has_sensor']:
            # Find sensor key for this LED
            sensor_key = None
            for key, sensor_config in config.SENSOR_DEVICES.items():
                if sensor_config['led_index'] == i:
                    sensor_key = key
                    break
            
            if sensor_key:
                # Get sensor data
                sensor_data = ble_scanner.get_sensor_data(sensor_key)
                
                # Calculate LED state with sensor effects
                final_rgb, final_brightness = sensor_logic.calculate_led_state(
                    i,
                    base_rgb,
                    base_brightness,
                    sensor_data,
                    global_mode
                )
            else:
                # No sensor found
                final_rgb = base_rgb
                final_brightness = base_brightness
        else:
            # Static room (no sensor)
            final_rgb = base_rgb
            final_brightness = base_brightness
        
        # Set LED
        led_controller.set_led(i, final_rgb, final_brightness, state['on'])
    
    # Write to LEDs
    led_controller.update()

def update_oled():
    """Update OLED display"""
    global current_page, oled_display, led_states, ble_scanner, global_mode, start_time
    
    if not oled_display or not oled_display.enabled:
        return
    
    uptime_sec = time.ticks_diff(time.ticks_ms(), start_time) // 1000
    ble_status = ble_scanner.get_connection_status()
    
    if current_page == 0:
        # Home overview
        room_states = []
        for i, state in enumerate(led_states):
            room_states.append({
                'name': state['room_name'],
                'brightness': state['brightness'],
                'has_sensor': state['has_sensor']
            })
        oled_display.show_home_overview(room_states, ble_status, global_mode)
    
    elif current_page == 1:
        # Room 1 detail (first sensor room)
        sensor_key = 'sensor_1'
        sensor_config = config.SENSOR_DEVICES[sensor_key]
        led_idx = sensor_config['led_index']
        state = led_states[led_idx]
        sensor_data = ble_scanner.get_sensor_data(sensor_key)
        
        oled_display.show_room_detail(
            state['room_name'],
            led_idx,
            state['rgb'],
            state['brightness'],
            sensor_data
        )
    
    elif current_page == 2:
        # Room 2 detail (second sensor room)
        sensor_key = 'sensor_2'
        sensor_config = config.SENSOR_DEVICES[sensor_key]
        led_idx = sensor_config['led_index']
        state = led_states[led_idx]
        sensor_data = ble_scanner.get_sensor_data(sensor_key)
        
        oled_display.show_room_detail(
            state['room_name'],
            led_idx,
            state['rgb'],
            state['brightness'],
            sensor_data
        )
    
    elif current_page == 3:
        # System status
        oled_display.show_system_status(ble_status, global_mode, uptime_sec)

def cycle_oled_page():
    """Cycle to next OLED page"""
    global current_page
    current_page = (current_page + 1) % 4  # 0-3 pages

def toggle_lights():
    """Toggle all lights on/off"""
    global lights_on
    lights_on = not lights_on
    print(f"Lights: {'ON' if lights_on else 'OFF'}")

def toggle_mode():
    """Toggle between auto and manual mode"""
    global global_mode
    global_mode = 'manual' if global_mode == 'auto' else 'auto'
    print(f"Mode: {global_mode.upper()}")

def check_ble_reconnect():
    """Check if BLE sensors need reconnection"""
    global last_scan_time, ble_scanner
    
    current_time = time.ticks_ms()
    if time.ticks_diff(current_time, last_scan_time) > (config.BLE_SCAN_INTERVAL * 1000):
        last_scan_time = current_time
        ble_scanner.reconnect_all()

def main_loop():
    """Main application loop"""
    global last_oled_update
    
    print("Starting main loop...")
    print("Press Ctrl+C to exit\n")
    
    loop_count = 0
    
    try:
        while True:
            loop_start = time.ticks_ms()
            
            # Update LEDs every cycle
            update_leds()
            
            # Update OLED at lower rate
            if time.ticks_diff(loop_start, last_oled_update) > config.OLED_UPDATE_RATE:
                update_oled()
                last_oled_update = loop_start
            
            # Check BLE reconnection
            check_ble_reconnect()
            
            # Debug output every 2 seconds
            loop_count += 1
            if loop_count % 40 == 0:  # Every 2 seconds at 50ms rate
                ble_status = ble_scanner.get_connection_status()
                connected = sum(1 for v in ble_status.values() if v)
                print(f"[{loop_count}] BLE: {connected}/{len(ble_status)} | Mode: {global_mode} | Lights: {'ON' if lights_on else 'OFF'} | Page: {current_page}")
                
                # Print sensor data if connected
                for key in ['sensor_1', 'sensor_2']:
                    if key in ble_status and ble_status[key]:
                        data = ble_scanner.get_sensor_data(key)
                        print(f"  {key}: Lux={data['luminosity']}, Temp={data['temperature']:.1f}°C, Hum={data['humidity']:.1f}%, Audio={data['audio_peak']}")
                
                # Garbage collection
                gc.collect()
            
            # Sleep to maintain update rate
            elapsed = time.ticks_diff(time.ticks_ms(), loop_start)
            sleep_time = config.UPDATE_RATE - elapsed
            if sleep_time > 0:
                time.sleep_ms(sleep_time)
    
    except KeyboardInterrupt:
        print("\n\nShutting down...")
        led_controller.clear()
        oled_display.clear()
        oled_display.show_text("Stopped", 0, 0)
        oled_display.update()
        print("Goodbye!")
    
    except Exception as e:
        print(f"\n\nERROR: {e}")
        if oled_display:
            oled_display.show_error(str(e))
        raise

# ============================================
# ENTRY POINT
# ============================================

if __name__ == '__main__':
    try:
        init_system()
        time.sleep(1)
        main_loop()
    except Exception as e:
        print(f"Fatal error: {e}")
        if led_controller:
            led_controller.clear()
        time.sleep(2)
        reset()


