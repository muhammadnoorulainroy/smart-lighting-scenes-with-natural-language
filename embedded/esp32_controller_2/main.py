"""
ESP32 #2 - Main Controller (WiFi + MQTT + UART)

NEW DUAL-ESP32 ARCHITECTURE:
- ESP32 #1: Dedicated BLE scanning (100% radio time)
- ESP32 #2: WiFi/MQTT + LED control + OLED (100% radio time)
- Communication: UART JSON messages

Features:
- MQTT bidirectional control (pub/sub)
- BLE sensor data via UART from ESP32 #1
- Zero WiFi/BLE radio conflicts!
- Local + remote control
- Environmental automation
- Real-time OLED display
"""
import uasyncio as asyncio
import time, gc, sys
import network
from machine import reset, Pin
try:
    import machine
    import esp32
except:
    pass

import config
from logger import log, log_err
from led_controller_async import AsyncLEDController
from oled_display_async import AsyncOLEDDisplay
from uart_receiver_async import UARTReceiver
from sensor_logic_async import AsyncSensorLogic
from mqtt_client_async import AsyncMQTTClient, SmartLightingMQTT

# Runtime config with backend overrides
from runtime_config import cfg

# Config bridge for provisioned WiFi settings
try:
    from config_bridge import get_config, factory_reset
    wifi_cfg = get_config()
except ImportError:
    wifi_cfg = config
    def factory_reset():
        return False

_SRC = "main"

def _cfg(name, default):
    """Get config value from RuntimeConfig (with backend overrides)."""
    val = getattr(cfg, name, None)
    return val if val is not None else default


# =========================
# SYSTEM STATE
# =========================
class SystemState:
    def __init__(self):
        self.global_mode = _cfg("GLOBAL_MODE", "auto")
        self.lights_on = True
        self.current_page = 0
        self.start_time = time.ticks_ms()
        
        # OLED power management
        self.oled_active = True
        self.oled_last_activity = time.ticks_ms()
        # Note: OLED_AUTO_SLEEP is checked dynamically in check_oled_sleep()
        self.oled_wake_requested = False  # Flag for button-triggered wake

        self.led_states = []
        num_leds = _cfg("NUM_LEDS", 5)
        default_colors = _cfg("DEFAULT_COLORS", [(255,255,255)] * num_leds)
        led_room_names = _cfg("LED_ROOM_NAMES", [])
        
        for i in range(num_leds):
            room_name = led_room_names[i] if i < len(led_room_names) else f"LED{i}"
            base_brightness = _cfg("DEFAULT_BRIGHTNESS", 10)
            self.led_states.append({
                "rgb": default_colors[i],
                "base_brightness": base_brightness,
                "brightness": base_brightness,
                "saturation": 100,  # Saturation percentage (affected by humidity)
                "color_temp": 4000,  # Color temperature in Kelvin (affected by temperature)
                "on": True,
                "has_sensor": False,
                "room_name": room_name,
            })
    
    def set_led_state(self, led_index, **kw):
        if 0 <= led_index < len(self.led_states):
            self.led_states[led_index].update(kw)
    
    def get_led_state(self, led_index):
        if 0 <= led_index < len(self.led_states):
            return self.led_states[led_index].copy()
        return None
    
    def wake_oled(self):
        """Wake OLED and reset activity timer"""
        if not self.oled_active:
            log(_SRC, "OLED waking up")
            self.oled_wake_requested = True  # Signal main loop to call oled.wake()
        self.oled_active = True
        self.oled_last_activity = time.ticks_ms()
    
    def check_oled_sleep(self):
        """Check if OLED should sleep due to inactivity"""
        # Check config dynamically (can be changed from backend)
        if not _cfg("OLED_AUTO_SLEEP", True):
            return False
        
        timeout_sec = _cfg("OLED_TIMEOUT", 15)
        timeout_ms = timeout_sec * 1000
        if self.oled_active and time.ticks_diff(time.ticks_ms(), self.oled_last_activity) > timeout_ms:
            self.oled_active = False
            log(_SRC, "OLED sleeping (power save)")
            return True
        return False
    
    def go_to_home_page(self):
        self.current_page = 0
        self.wake_oled()
    
    def next_page(self):
        max_pages = 5
        self.current_page = (self.current_page + 1) % max_pages
        self.wake_oled()
    
    def prev_page(self):
        max_pages = 5
        self.current_page = (self.current_page - 1) % max_pages
        self.wake_oled()
    
    def toggle_lights(self):
        self.lights_on = not self.lights_on
        log(_SRC, f"Lights {'ON' if self.lights_on else 'OFF'}")
        self.wake_oled()
    
    def toggle_mode(self):
        self.global_mode = "manual" if self.global_mode == "auto" else "auto"
        log(_SRC, f"Mode: {self.global_mode}")
        self.wake_oled()
    
    def increase_brightness(self, led_index, step=10):
        st = self.get_led_state(led_index)
        if st:
            new_br = min(100, st["base_brightness"] + step)
            self.set_led_state(led_index, base_brightness=new_br)
            log(_SRC, f"LED{led_index} brightness: {new_br}%")
        self.wake_oled()


# Global state
state = SystemState()

# Global MQTT reference for ack publishing (set in init_mqtt)
_mqtt_sl = None

# =========================
# BUTTON HANDLERS
# =========================
button_a_last = 0
button_b_last = 0
button_c_last = 0
DEBOUNCE_MS = 200

def button_a_handler(pin):
    global button_a_last
    now = time.ticks_ms()
    if time.ticks_diff(now, button_a_last) < DEBOUNCE_MS:
        return
    button_a_last = now
    # If OLED sleeping, just wake it (don't navigate)
    if not state.oled_active:
        state.wake_oled()
    else:
        state.go_to_home_page()

def button_b_handler(pin):
    global button_b_last
    now = time.ticks_ms()
    if time.ticks_diff(now, button_b_last) < DEBOUNCE_MS:
        return
    button_b_last = now
    # If OLED sleeping, just wake it (don't navigate)
    if not state.oled_active:
        state.wake_oled()
    else:
        state.next_page()

def button_c_handler(pin):
    global button_c_last
    now = time.ticks_ms()
    if time.ticks_diff(now, button_c_last) < DEBOUNCE_MS:
        return
    button_c_last = now
    # If OLED sleeping, just wake it (don't toggle)
    if not state.oled_active:
        state.wake_oled()
    else:
        state.toggle_lights()


# =========================
# MQTT COMMAND HANDLERS
# =========================
def _room_to_led_index(room_name):
    """Convert room name to LED index."""
    room_names = _cfg("LED_ROOM_NAMES", [])
    room_lower = room_name.lower().replace("_", " ").replace("-", " ")
    for i, name in enumerate(room_names):
        if name.lower() == room_lower:
            return i
    return None


async def on_mqtt_command(topic, msg):
    """Handle MQTT commands"""
    try:
        # Skip our own state publications (not commands)
        if "/state" in topic or "/status/" in topic or "/sensor/" in topic:
            return
        
        # Log actual commands only
        if "/command/" in topic or "/led/" in topic or "/room/" in topic or "/scene/" in topic:
            log(_SRC, "MQTT cmd: {} = {}".format(topic.split("/")[-2:], msg))
        
        parts = topic.split("/")
        
        # Global lights control: smartlight/command/lights
        if topic.endswith("/lights"):
            if msg == "on":
                state.lights_on = True
            elif msg == "off":
                state.lights_on = False
            elif msg == "toggle":
                state.toggle_lights()
        
        # Global mode: smartlight/command/mode
        elif topic.endswith("/mode"):
            if msg in ("auto", "manual"):
                state.global_mode = msg
        
        # Global brightness: smartlight/command/brightness
        elif topic.endswith("/command/brightness"):
            try:
                br = int(msg)
                br = max(0, min(100, br))
                for i in range(_cfg("NUM_LEDS", 5)):
                    state.set_led_state(i, base_brightness=br)
            except ValueError:
                pass
        
        # Per-LED control: smartlight/led/{index}/{action}
        # Actions: power, brightness, color, set (JSON command)
        elif "/led/" in topic and len(parts) >= 4:
            try:
                led_idx = int(parts[2])
                action = parts[3]
                if 0 <= led_idx < _cfg("NUM_LEDS", 5):
                    if action == "power":
                        state.set_led_state(led_idx, on=(msg == "on"))
                    elif action == "brightness":
                        br = max(0, min(100, int(msg)))
                        state.set_led_state(led_idx, base_brightness=br)
                    elif action == "color":
                        # Expect "r,g,b" format
                        r, g, b = [int(x) for x in msg.split(",")]
                        state.set_led_state(led_idx, rgb=(r, g, b))
                    elif action == "set":
                        # JSON command: {"on": true, "rgb": [r,g,b], "brightness": 50, "color_temp": 4000, "mode": "manual", "correlationId": "uuid"}
                        import json
                        cmd = json.loads(msg)
                        log(_SRC, f"LED {led_idx} SET cmd: {cmd}")
                        
                        # Extract correlation ID for acknowledgment
                        correlation_id = cmd.get("correlationId")
                        
                        # Set mode to manual if specified
                        if cmd.get("mode") == "manual":
                            state.global_mode = "manual"
                        
                        # Apply power state
                        if "on" in cmd:
                            state.set_led_state(led_idx, on=cmd["on"])
                        
                        # Apply RGB color
                        if "rgb" in cmd:
                            rgb = cmd["rgb"]
                            if isinstance(rgb, list) and len(rgb) >= 3:
                                state.set_led_state(led_idx, rgb=(rgb[0], rgb[1], rgb[2]))
                        
                        # Apply brightness (as base_brightness for manual control)
                        if "brightness" in cmd:
                            br = max(0, min(100, int(cmd["brightness"])))
                            state.set_led_state(led_idx, base_brightness=br, brightness=br)
                        
                        # Apply color temperature (store for reference, affects color calculation)
                        if "color_temp" in cmd:
                            ct = int(cmd["color_temp"])
                            state.set_led_state(led_idx, color_temp=ct)
                        
                        # Handle mode switching
                        if cmd.get("mode") == "manual":
                            # Disable sensor-based auto adjustments for this LED in manual mode
                            state.set_led_state(led_idx, has_sensor=False)
                        elif cmd.get("mode") == "auto":
                            # Re-enable sensor-based auto adjustments
                            # LEDs 0 (living room) and 1 (bedroom) have sensors
                            has_sens = led_idx in [0, 1]
                            state.set_led_state(led_idx, has_sensor=has_sens)
                            log(_SRC, f"LED {led_idx} switched to AUTO mode (sensor: {has_sens})")
                        
                        # Send acknowledgment if correlation ID present
                        if correlation_id and _mqtt_sl:
                            try:
                                await _mqtt_sl.publish_scene_ack(correlation_id, led_idx, success=True)
                            except Exception as ack_err:
                                log(_SRC, f"ACK send failed: {ack_err}")
            except (ValueError, IndexError) as e:
                log(_SRC, f"LED cmd error: {e}")
        
        # Per-room control: smartlight/room/{name}/{action}
        # Room names: living_room, bedroom, kitchen, bath, hallway
        elif "/room/" in topic and len(parts) >= 4:
            room_name = parts[2]
            action = parts[3]
            led_idx = _room_to_led_index(room_name)
            if led_idx is not None:
                try:
                    if action == "power":
                        state.set_led_state(led_idx, on=(msg == "on"))
                    elif action == "brightness":
                        br = max(0, min(100, int(msg)))
                        state.set_led_state(led_idx, base_brightness=br)
                    elif action == "color":
                        r, g, b = [int(x) for x in msg.split(",")]
                        state.set_led_state(led_idx, color=(r, g, b))
                except ValueError:
                    pass
        
        # Global mode command: smartlighting/mode/set
        elif topic.endswith("/mode/set") or topic.endswith("/mode"):
            try:
                if msg in ["auto", "manual"]:
                    state.global_mode = msg
                    log(_SRC, f"Global mode set to: {msg}")
                    # For auto mode, re-enable sensors on LEDs 0 and 1
                    if msg == "auto":
                        for i in range(5):
                            has_sens = i in [0, 1]  # Living room and bedroom have sensors
                            state.set_led_state(i, has_sensor=has_sens)
                        log(_SRC, "All LEDs switched to AUTO mode with sensor detection")
                    else:
                        # Manual mode - disable sensor effects
                        for i in range(5):
                            state.set_led_state(i, has_sensor=False)
                        log(_SRC, "All LEDs switched to MANUAL mode")
            except Exception as e:
                log(_SRC, f"Mode command error: {e}")
        
        # Config updates from backend: smartlighting/config/update or smartlighting/config/{category}
        elif "/config/" in topic:
            # Skip our own config request message
            if topic.endswith("/config/request"):
                return
            
            try:
                import json
                # Skip non-JSON payloads
                if not msg or msg in ("get", "full", "refresh"):
                    return
                config_data = json.loads(msg) if isinstance(msg, str) else msg
                
                if topic.endswith("/config/update"):
                    # Full config update
                    cfg.update(config_data)
                    log(_SRC, f"Full config received from backend")
                else:
                    # Category update (e.g., /config/lighting)
                    parts = topic.split("/")
                    category = parts[-1] if parts else "unknown"
                    cfg.update_category(category, config_data)
                    log(_SRC, f"Config category '{category}' updated")
            except Exception as e:
                log(_SRC, f"Config update error: {e} (msg={msg[:50] if msg else 'empty'})")
        
        elif topic.endswith("/factory_reset"):
            if msg == "confirm":
                log(_SRC, "Factory reset requested via MQTT!")
                factory_reset()
                await asyncio.sleep(1)
                machine.reset()
        
        elif topic.endswith("/reboot"):
            if msg == "confirm":
                log(_SRC, "Reboot requested via MQTT!")
                await asyncio.sleep(1)
                machine.reset()
        
        # Wake OLED on any command
        state.wake_oled()
    
    except Exception as e:
        log_err(_SRC, f"MQTT command error: {e}")


# =========================
# INIT FUNCTIONS
# =========================
async def init_wifi():
    if not _cfg("WIFI_ENABLED", True):
        log(_SRC, "WiFi disabled")
        return None

    # Use provisioned config (NVS) or fall back to config.py defaults
    wifi_ssid = getattr(wifi_cfg, 'WIFI_SSID', None) or config.WIFI_SSID
    wifi_password = getattr(wifi_cfg, 'WIFI_PASSWORD', None) or config.WIFI_PASSWORD
    
    if not wifi_ssid:
        log_err(_SRC, "No WiFi SSID configured!")
        return None

    log(_SRC, f"WiFi connecting to: {wifi_ssid}")
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)

    if not wlan.isconnected():
        wlan.connect(wifi_ssid, wifi_password)
        t0 = time.time()
        while not wlan.isconnected():
            if time.time() - t0 > _cfg("WIFI_TIMEOUT", 15):
                log_err(_SRC, "WiFi timeout")
                return None
            await asyncio.sleep_ms(400)

    ip = wlan.ifconfig()[0]
    log(_SRC, f"WiFi connected: {ip}")

    # Power save mode (not needed with dual-ESP32!)
    try:
        wlan.config(pm=0xa11140)  # Min modem sleep
        log(_SRC, "WiFi: Min modem sleep (balanced)")
    except Exception as e:
        log(_SRC, f"WiFi power config: {e}")
    
    # Sync time with NTP (France: UTC+1 winter, UTC+2 summer)
    try:
        import ntptime
        from machine import RTC
        import socket
        
        log(_SRC, "Syncing time with NTP...")
        
        # Try multiple NTP servers with explicit timeout
        ntp_servers = ["pool.ntp.org", "time.google.com", "time.cloudflare.com"]
        synced = False
        utc_time = 0
        
        for server in ntp_servers:
            try:
                ntptime.host = server
                ntptime.timeout = 5  # 5 second timeout
                ntptime.settime()
                await asyncio.sleep_ms(100)
                
                utc_time = time.time()
                # MicroPython epoch is 2000-01-01
                # 750000000 = ~2023 in MicroPython epoch
                if utc_time > 750000000:
                    synced = True
                    log(_SRC, f"NTP OK from {server}")
                    break
                else:
                    log(_SRC, f"NTP {server}: time={utc_time} (invalid)")
            except Exception as e:
                log(_SRC, f"NTP {server}: {e}")
                continue
        
        if not synced:
            # Fallback: set a reasonable default time
            log(_SRC, "⚠️ NTP failed - setting default time")
            rtc = RTC()
            # Set to 2025-11-28 12:00:00
            rtc.datetime((2025, 11, 28, 4, 12, 0, 0, 0))
            return wlan
        
        # France timezone: UTC+1 (winter) or UTC+2 (summer DST)
        tm_utc = time.localtime(utc_time)
        month = tm_utc[1]
        # Approximate: April-October = summer time (UTC+2), else winter (UTC+1)
        if 4 <= month <= 10:
            tz_offset = 7200  # UTC+2 (summer)
        else:
            tz_offset = 3600  # UTC+1 (winter)
        
        france_time_seconds = utc_time + tz_offset
        tm = time.localtime(france_time_seconds)
        
        rtc = RTC()
        rtc.datetime((tm[0], tm[1], tm[2], tm[6], tm[3], tm[4], tm[5], 0))
        
        tz_str = "UTC+2" if tz_offset == 7200 else "UTC+1"
        log(_SRC, f"✓ Time: {tm[2]:02d}/{tm[1]:02d}/{tm[0]} {tm[3]:02d}:{tm[4]:02d} ({tz_str})")
    except Exception as e:
        log_err(_SRC, f"NTP sync failed: {e}")

    return wlan


async def init_mqtt():
    """Initialize MQTT client"""
    global _mqtt_sl
    from umqtt.simple import MQTTClient as SyncMQTTClient
    
    if not _cfg("MQTT_ENABLED", True):
        log(_SRC, "MQTT disabled")
        return None, None
    
    # WiFi comes from provisioning, MQTT from hardcoded config.py
    mqtt_broker = cfg.MQTT_BROKER
    mqtt_port = cfg.MQTT_PORT
    mqtt_user = cfg.MQTT_USER
    mqtt_password = cfg.MQTT_PASSWORD
    mqtt_client_id = cfg.MQTT_CLIENT_ID
    mqtt_base_topic = cfg.MQTT_BASE_TOPIC
    
    if not mqtt_broker:
        log_err(_SRC, "No MQTT broker configured!")
        return None, None
    
    try:
        # Force garbage collection before network operations
        gc.collect()
        
        # Wait for network stack to stabilize
        await asyncio.sleep(2)
        
        log(_SRC, f"Connecting to MQTT: {mqtt_broker}:{mqtt_port}")
        log(_SRC, f"  client_id={mqtt_client_id}, user={mqtt_user}")
        
        # Convert empty strings to None for umqtt
        user = mqtt_user if mqtt_user else None
        passwd = mqtt_password if mqtt_password else None
        lwt_topic = f"{mqtt_base_topic}/status/online"
        
        # Use synchronous connection (bypass async wrapper issues)
        connected = False
        sync_client = None
        
        for attempt in range(3):
            try:
                gc.collect()
                sync_client = SyncMQTTClient(
                    mqtt_client_id,
                    mqtt_broker,
                    port=mqtt_port,
                    user=user,
                    password=passwd,
                    keepalive=_cfg("MQTT_KEEPALIVE", 60)
                )
                sync_client.set_last_will(lwt_topic, "offline", retain=True)
                sync_client.connect(clean_session=True)
                connected = True
                log(_SRC, f"MQTT connected on attempt {attempt+1}")
                break
            except Exception as e:
                log(_SRC, f"MQTT attempt {attempt+1}/3 failed: {e}")
                if sync_client:
                    try:
                        sync_client.disconnect()
                    except:
                        pass
                await asyncio.sleep(1)
        
        if not connected:
            log_err(_SRC, "MQTT connection failed after 3 attempts")
            return None, None
        
        # Wrap in async client for compatibility with rest of code
        mqtt = AsyncMQTTClient(
            client_id=mqtt_client_id,
            broker=mqtt_broker,
            port=mqtt_port,
            user=mqtt_user,
            password=mqtt_password,
            keepalive=_cfg("MQTT_KEEPALIVE", 60)
        )
        mqtt._client = sync_client
        mqtt.connected = True
        
        # Create smart lighting interface
        sl = SmartLightingMQTT(mqtt, base_topic=mqtt_base_topic)
        
        # Store global reference for ack publishing in command handler
        _mqtt_sl = sl
        
        # Subscribe to commands
        sl.subscribe_commands(on_mqtt_command)
        
        # Publish online status
        await sl.publish_online_status(online=True)
        
        log(_SRC, f"MQTT ready: {mqtt_broker}")
        
        # Request config from backend
        await asyncio.sleep_ms(500)  # Small delay to ensure subscriptions are active
        await sl.request_config()
        return mqtt, sl
    
    except Exception as e:
        log_err(_SRC, f"MQTT init failed: {e}")
        return None, None



async def init_hardware():
    log(_SRC, "=" * 40)
    log(_SRC, "ESP32 #2 - Main Controller (UART)")
    log(_SRC, "=" * 40)

    # LEDs
    led = AsyncLEDController(_cfg("LED_PIN", 13), _cfg("NUM_LEDS", 5))
    await led.test_pattern()
    log(_SRC, f"LEDs ready: {_cfg('NUM_LEDS',5)} on GPIO {_cfg('LED_PIN',13)}")

    # OLED
    oled = AsyncOLEDDisplay(
        _cfg("OLED_SDA",23), _cfg("OLED_SCL",22),
        _cfg("OLED_WIDTH",128), _cfg("OLED_HEIGHT",64),
        _cfg("OLED_ADDR",0x3C),
    )
    await oled.init_async()
    
    # OLED buttons
    try:
        button_a_pin = _cfg("BUTTON_OLED_A", 15)
        button_a = Pin(button_a_pin, Pin.IN, Pin.PULL_UP)
        button_a.irq(trigger=Pin.IRQ_FALLING, handler=button_a_handler)
        log(_SRC, f"Button A ready on GPIO {button_a_pin}")
        
        button_b_pin = _cfg("BUTTON_OLED_B", 32)
        button_b = Pin(button_b_pin, Pin.IN, Pin.PULL_UP)
        button_b.irq(trigger=Pin.IRQ_FALLING, handler=button_b_handler)
        log(_SRC, f"Button B ready on GPIO {button_b_pin}")
        
        button_c_pin = _cfg("BUTTON_OLED_C", 14)
        button_c = Pin(button_c_pin, Pin.IN, Pin.PULL_UP)
        button_c.irq(trigger=Pin.IRQ_FALLING, handler=button_c_handler)
        log(_SRC, f"Button C ready on GPIO {button_c_pin}")
    except Exception as e:
        log(_SRC, f"Button setup: {e}")

    # WiFi (no conflicts with BLE now!)
    wlan = None
    if _cfg("WIFI_ENABLED", True):
        wlan = await init_wifi()
    else:
        log(_SRC, "WiFi disabled")
    
    # MQTT - connect BEFORE starting async tasks to avoid interference
    mqtt, sl = await init_mqtt()

    # UART Receiver (replaces BLE scanner!)
    uart = None
    uart_rx_pin = _cfg("UART_RX_PIN", 16)
    uart_baudrate = _cfg("UART_BAUDRATE", 115200)
    
    try:
        gc.collect()
        log(_SRC, "Initializing UART receiver...")
        uart = UARTReceiver(
            uart_id=2,
            baudrate=uart_baudrate,
            rx_pin=uart_rx_pin,
            tx_pin=17  # TX not used, but required by UART init
        )
        # Start UART receive task AFTER MQTT is connected
        asyncio.create_task(uart.receive_task())
        log(_SRC, "UART receiver ready on GPIO {}".format(uart_rx_pin))
    except Exception as e:
        log_err(_SRC, f"UART init failed: {e}")

    # Sensor logic (uses RuntimeConfig for dynamic settings from backend)
    logic = AsyncSensorLogic(cfg)
    log(_SRC, "Sensor logic ready")
    
    return led, oled, uart, logic, wlan, mqtt, sl


# =========================
# MAIN LOOP
# =========================
async def main_loop():
    led, oled, uart, logic, wlan, mqtt, sl = await init_hardware()
    
    # Loop timers
    last_led = time.ticks_ms()
    last_oled = time.ticks_ms()
    last_mqtt_pub = time.ticks_ms()
    last_heartbeat = time.ticks_ms()
    
    # Intervals
    led_interval = 16  # 60Hz
    oled_interval = 200  # 5Hz
    mqtt_pub_interval = 60000  # Publish every 1 minute (backup, main publish is on-change)
    heartbeat_interval = 10000  # 10s
    
    frame_count = 0
    oled_count = 0
    mqtt_pub_count = 0
    
    log(_SRC, "\n🚀 Main loop starting...\n")
    
    while True:
        try:
            now = time.ticks_ms()
            
            # === LED Update (60Hz) ===
            if time.ticks_diff(now, last_led) >= led_interval:
                last_led = now
                for i in range(_cfg("NUM_LEDS",5)):
                    st = state.get_led_state(i)
                    old_br = st.get("brightness", 0)
                    old_on = st.get("on", True)
                    
                    if not state.lights_on:
                        await led.set_led(i, (0,0,0), 0, False)
                        state.set_led_state(i, brightness=0, saturation=100, color_temp=4000)
                        new_br, new_on = 0, False
                    else:
                        base_br = st.get("base_brightness", st["brightness"])
                        
                        # Apply MAX_BRIGHTNESS limit from config to ALL LEDs
                        max_br = _cfg("MAX_BRIGHTNESS", 100)
                        if max_br is None:
                            max_br = 100
                        
                        if st["has_sensor"] and state.global_mode == "auto" and uart:
                            rgb, br, sat, ct = await logic.calculate_led_state(i, st["rgb"], base_br)
                        else:
                            # LEDs without sensors still respect MAX_BRIGHTNESS
                            capped_br = min(base_br, max_br)
                            rgb, br, sat, ct = st["rgb"], capped_br, 100, 4000
                        await led.set_led(i, rgb, br, st["on"])
                        state.set_led_state(i, brightness=br, saturation=sat, color_temp=ct, rgb=rgb)
                        new_br, new_on = br, st["on"]
                    
                    # Publish immediately if state changed (brightness or on/off)
                    if mqtt and mqtt.connected and sl and (new_br != old_br or new_on != old_on):
                        updated_st = state.get_led_state(i)
                        await sl.publish_led_state(i, updated_st)
                        
                await led.update()
                frame_count += 1
            await asyncio.sleep_ms(0)
            
            # === OLED Update (5Hz) ===
            if time.ticks_diff(now, last_oled) >= oled_interval:
                last_oled = now
                
                # Check for button-triggered wake
                if state.oled_wake_requested:
                    state.oled_wake_requested = False
                    oled.wake()
                
                # Check for sleep transition
                was_active = state.oled_active
                state.check_oled_sleep()
                
                # Handle sleep transition
                if was_active and not state.oled_active:
                    # Just went to sleep - turn off display
                    oled.sleep()
                    oled_count = 0
                
                if state.oled_active:
                    # Get BLE status directly from ESP32 #1 via UART
                    expected_sensors = len(_cfg("LED_SENSOR_MAPPING", {}))
                    if uart:
                        ble_status = uart.get_ble_status()
                        # Override expected with our config
                        ble_status["expected"] = expected_sensors
                    else:
                        ble_status = {"connected": 0, "ready": 0, "expected": expected_sensors}
                    
                    room_states = [
                        {"name": s["room_name"], "brightness": s["brightness"], "has_sensor": s["has_sensor"]}
                        for s in state.led_states
                    ]
                    
                    # Get sensor data for detail pages
                    sensor_data = {}
                    if uart:
                        for name, data in uart.get_all_sensors().items():
                            sensor_data[name] = data
                    
                    await oled.show_page(
                        page=state.current_page,
                        room_states=room_states,
                        ble_status=ble_status,
                        sensor_data=sensor_data,
                        global_mode=state.global_mode,
                        uptime_s=time.ticks_diff(now, state.start_time) // 1000
                    )
                    oled_count += 1
            await asyncio.sleep_ms(0)
            
            # === Update Sensor Logic (from UART data) ===
            if uart:
                # Get latest sensor data from UART
                sensors = uart.get_all_sensors()
                
                if sensors:
                    sensor_map = _cfg("LED_SENSOR_MAPPING", {})
                    for name, data in sensors.items():
                        # Map sensor to LED
                        for led_idx, sensor_name in sensor_map.items():
                            if sensor_name in name:
                                state.set_led_state(led_idx, has_sensor=True)
                                # Update logic with sensor data (use sensor NAME as key, not LED index!)
                                await logic.update_from_sensors({name: data})
                                break
                
                # Cleanup stale sensors (every 10s)
                if time.ticks_diff(now, last_heartbeat) >= 10000:
                    stale = uart.cleanup_stale_sensors(timeout_s=30)
                    if stale > 0:
                        log(_SRC, f"Removed {stale} stale sensor(s)")
            await asyncio.sleep_ms(0)
            
            # === MQTT Loop (continuous, but non-blocking) ===
            if mqtt and mqtt.connected:
                await mqtt.loop(duration_ms=10)  # Check messages briefly
            await asyncio.sleep_ms(0)
            
            # === MQTT Publish (5s) ===
            if mqtt and mqtt.connected and sl and time.ticks_diff(now, last_mqtt_pub) >= mqtt_pub_interval:
                last_mqtt_pub = now
                mqtt_pub_count += 1
                
                # Publish sensor data
                if uart:
                    for name, data in uart.get_all_sensors().items():
                        await sl.publish_sensor_data(name, data)
                
                # Publish LED states
                for i in range(_cfg("NUM_LEDS", 5)):
                    st = state.get_led_state(i)
                    await sl.publish_led_state(i, st)
                
                # Publish system state
                await sl.publish_system_state({
                    "mode": state.global_mode,
                    "lights_on": state.lights_on,
                    "uptime_s": time.ticks_diff(now, state.start_time) // 1000,
                    "memory_free": gc.mem_free(),
                })
            await asyncio.sleep_ms(0)
            
            # === Heartbeat (10s) ===
            if time.ticks_diff(now, last_heartbeat) >= heartbeat_interval:
                last_heartbeat = now
                
                # Get BLE status from ESP32 #1
                ble_conn = 0
                ble_ready = 0
                if uart:
                    ble_status = uart.get_ble_status()
                    ble_conn = ble_status.get("connected", 0)
                    ble_ready = ble_status.get("ready", 0)
                mqtt_status = "✓" if (mqtt and mqtt.connected) else "✗"
                
                log(_SRC, "Up:{}s Mem:{}B BLE:{}/{} MQTT:{}".format(
                    time.ticks_diff(now, state.start_time)//1000,
                    gc.mem_free(),
                    ble_ready,
                    ble_conn,
                    mqtt_status
                ))
            
            await asyncio.sleep_ms(5)
            
        except KeyboardInterrupt:
            raise
        except Exception as e:
            log_err(_SRC, f"Main loop error: {e}")
            import sys
            sys.print_exception(e)
            await asyncio.sleep(5)


# =========================
# ENTRY POINT
# =========================
async def main():
    try:
        await main_loop()
    except KeyboardInterrupt:
        log(_SRC, "\n👋 Shutting down...")
    except Exception as e:
        log_err(_SRC, f"Fatal error: {e}")
        import sys
        sys.print_exception(e)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nStopped by user")



