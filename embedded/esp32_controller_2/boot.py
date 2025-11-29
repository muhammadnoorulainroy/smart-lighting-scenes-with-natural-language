# boot.py - ESP32 #2 (Main Controller)
# Executed on every boot before main.py

import gc
import micropython

gc.collect()
gc.threshold(gc.mem_free() // 4 + gc.mem_alloc())

print("\n" + "=" * 50)
print("  SmartLight ESP32 Controller - Boot")
print("=" * 50)
print("  Free memory: {} bytes".format(gc.mem_free()))

# Factory Reset: Hold OLED Button A for 5 seconds during boot
# This only erases WiFi/MQTT config
#
# Alternative methods:
# - MQTT Command: Publish "CONFIRM" to smartlight/cmd/factory_reset
# - REPL: import os; os.remove("config_saved.json"); import machine; machine.reset()

# Load button config (with fallback defaults)
try:
    import config
    FACTORY_RESET_PIN = getattr(config, 'FACTORY_RESET_BUTTON', 15)
    FACTORY_RESET_HOLD_SECONDS = getattr(config, 'FACTORY_RESET_HOLD_SECONDS', 5)
except ImportError:
    FACTORY_RESET_PIN = 15  # GPIO15 = OLED Button A (default)
    FACTORY_RESET_HOLD_SECONDS = 5

def check_button_factory_reset(pin_num, hold_seconds):
    """Check if button is held during boot for factory reset."""
    from machine import Pin
    import time
    
    btn = Pin(pin_num, Pin.IN, Pin.PULL_UP)
    
    # Button not pressed (HIGH = released for active-low buttons)
    if btn.value() == 1:
        return False
    
    print("[BOOT] Button A detected! Hold for {}s to factory reset WiFi config...".format(hold_seconds))
    
    start = time.time()
    dots = 0
    while btn.value() == 0:
        elapsed = time.time() - start
        
        # Show progress
        if int(elapsed) > dots:
            dots = int(elapsed)
            print("[BOOT] {} / {} seconds...".format(dots, hold_seconds))
        
        if elapsed >= hold_seconds:
            print("[BOOT] Factory reset triggered! Clearing WiFi config...")
            try:
                from config_manager import config_manager
                config_manager.clear()
                print("[BOOT] Config cleared. Rebooting into setup mode...")
                time.sleep(1)
                import machine
                machine.reset()
            except Exception as e:
                print("[BOOT] Reset failed:", e)
            return True
        
        time.sleep(0.1)
    
    print("[BOOT] Button released. Normal boot continues.")
    return False

try:
    check_button_factory_reset(FACTORY_RESET_PIN, FACTORY_RESET_HOLD_SECONDS)
except Exception as e:
    print("[BOOT] Factory reset check skipped:", e)

# Check if device needs provisioning
try:
    from config_manager import config_manager
    
    if not config_manager.is_provisioned():
        print("[BOOT] Device not provisioned - starting setup mode")
        
        # Initialize OLED for provisioning display (optional)
        oled = None
        try:
            from machine import Pin, SoftI2C
            import sh1107
            i2c = SoftI2C(scl=Pin(22), sda=Pin(23))
            oled = sh1107.SH1107_I2C(128, 64, i2c, address=0x3C)
            oled.fill(0)
            oled.text("Starting Setup", 8, 24)
            oled.show()
        except Exception as e:
            print("[BOOT] OLED init skipped:", e)
        
        # Start provisioning (blocks until complete)
        from wifi_provisioning import start_provisioning
        start_provisioning(oled=oled)
        
        # If we reach here, provisioning was cancelled - reboot
        import machine
        machine.reset()
    else:
        ssid, _ = config_manager.get_wifi_credentials()
        print("[BOOT] Device provisioned - WiFi SSID: {}".format(ssid))

except ImportError as e:
    print("[BOOT] Config modules not found:", e)
    print("[BOOT] Continuing with hardcoded config...")
except Exception as e:
    print("[BOOT] Provisioning check failed:", e)

gc.collect()
print("[BOOT] Starting main.py...")
print("=" * 50 + "\n")
