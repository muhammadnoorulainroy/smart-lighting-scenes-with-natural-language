"""
RuntimeConfig - Dynamic configuration wrapper for ESP32

This module provides a runtime configuration layer that:
1. Uses config.py values as defaults
2. Accepts overrides from backend via MQTT
3. Provides a unified interface for all configuration access

Usage:
    from runtime_config import cfg
    
    # Access config (checks overrides first, then config.py)
    brightness = cfg.MAX_BRIGHTNESS
    
    # Update from backend
    cfg.update({"lighting": {"maxBrightness": 80}})
"""

import config as _defaults

# Logging
def log(src, msg):
    try:
        import time
        t = time.ticks_ms() // 1000
        print(f"[{t:5d}s][{src}] {msg}")
    except:
        print(f"[runtime_config] {msg}")

_SRC = "runtime_config"


class RuntimeConfig:
    """
    Dynamic configuration that wraps config.py with runtime overrides.
    
    Backend sends updates via MQTT in format:
    {
        "lighting": {"globalMode": "auto", "maxBrightness": 100, ...},
        "climate": {"tempMin": 20, ...},
        "audio": {"discoEnabled": true, ...},
        "display": {"oledAutoSleep": true, ...},
        "mqtt": {"publishInterval": 60000, ...}
    }
    """
    
    # Mapping from backend camelCase keys to config.py UPPER_SNAKE_CASE
    _KEY_MAP = {
        # Lighting
        "globalMode": "GLOBAL_MODE",
        "autoDimEnabled": "AUTO_DIM_ENABLED",
        "sensorOverrideEnabled": "SENSOR_OVERRIDE_ENABLED",
        "minBrightness": "MIN_BRIGHTNESS",
        "maxBrightness": "MAX_BRIGHTNESS",
        "luxMin": "LUX_MIN",
        "luxMax": "LUX_MAX",
        
        # Climate
        "tempMin": "TEMP_MIN",
        "tempMax": "TEMP_MAX",
        "tempBlendStrength": "TEMP_BLEND_STRENGTH",
        "humidityMin": "HUMIDITY_MIN",
        "humidityMax": "HUMIDITY_MAX",
        "saturationAtMinHumidity": "SATURATION_AT_MIN_HUMIDITY",
        "saturationAtMaxHumidity": "SATURATION_AT_MAX_HUMIDITY",
        
        # Audio
        "discoEnabled": "DISCO_ENABLED",
        "audioThreshold": "AUDIO_THRESHOLD",
        "discoDuration": "AUDIO_DISCO_DURATION",
        "discoSpeed": "AUDIO_DISCO_SPEED",
        "flashBrightness": "AUDIO_FLASH_BRIGHTNESS",
        
        # Display
        "oledAutoSleep": "OLED_AUTO_SLEEP",
        "oledTimeout": "OLED_TIMEOUT",
        "showSensorData": "SHOW_SENSOR_DATA",
        "showTime": "SHOW_TIME",
        
        # MQTT
        "publishInterval": "MQTT_PUB_INTERVAL",
        "heartbeatInterval": "HEARTBEAT_INTERVAL",
    }
    
    def __init__(self):
        self._overrides = {}
        self._initialized = False
        log(_SRC, "RuntimeConfig initialized (using config.py defaults)")
    
    # Keys that need conversion from 0-100 percentage to 0-1 decimal
    _PERCENTAGE_TO_DECIMAL_KEYS = {
        "SATURATION_AT_MIN_HUMIDITY",
        "SATURATION_AT_MAX_HUMIDITY",
        "TEMP_BLEND_STRENGTH",
    }
    
    def _convert_value(self, key, value):
        """Convert backend values to config.py compatible format."""
        if key in self._PERCENTAGE_TO_DECIMAL_KEYS:
            # Convert 0-100 percentage to 0-1 decimal
            if isinstance(value, (int, float)) and value > 1:
                return value / 100.0
        return value
    
    def __getattr__(self, name):
        """
        Get config value. Checks overrides first, then falls back to config.py.
        
        Args:
            name: Config key in UPPER_SNAKE_CASE (e.g., MAX_BRIGHTNESS)
        
        Returns:
            Config value (override if set, otherwise default from config.py)
        """
        # Avoid recursion on internal attributes
        if name.startswith('_'):
            raise AttributeError(name)
        
        # Check overrides first
        if name in self._overrides:
            return self._overrides[name]
        
        # Fall back to config.py
        return getattr(_defaults, name, None)
    
    def get(self, name, default=None):
        """Safely get config value with default."""
        try:
            val = getattr(self, name)
            return val if val is not None else default
        except AttributeError:
            return default
    
    def update(self, config_data):
        """
        Update configuration from backend data.
        
        Args:
            config_data: Dict from backend, can be:
                - Full config: {"lighting": {...}, "climate": {...}, ...}
                - Single category: {"maxBrightness": 80, ...}
        """
        if not config_data:
            return
        
        updates = {}
        
        # Check if this is a full config (has category keys) or single category
        categories = ["lighting", "climate", "audio", "display", "mqtt"]
        is_full_config = any(cat in config_data for cat in categories)
        
        if is_full_config:
            # Full config with categories
            for category, settings in config_data.items():
                if isinstance(settings, dict):
                    for key, value in settings.items():
                        snake_key = self._KEY_MAP.get(key, key.upper())
                        # Convert percentage values to decimals for config.py compatibility
                        value = self._convert_value(snake_key, value)
                        updates[snake_key] = value
        else:
            # Single category settings
            for key, value in config_data.items():
                snake_key = self._KEY_MAP.get(key, key.upper())
                # Convert percentage values to decimals for config.py compatibility
                value = self._convert_value(snake_key, value)
                updates[snake_key] = value
        
        if updates:
            self._overrides.update(updates)
            self._initialized = True
            log(_SRC, f"Config updated: {list(updates.keys())}")
            # Debug: log brightness limits
            if "MAX_BRIGHTNESS" in updates or "MIN_BRIGHTNESS" in updates:
                log(_SRC, f"Brightness limits: MIN={updates.get('MIN_BRIGHTNESS', 'n/a')}, MAX={updates.get('MAX_BRIGHTNESS', 'n/a')}")
    
    def update_category(self, category, settings):
        """Update a specific category."""
        if isinstance(settings, dict):
            for key, value in settings.items():
                snake_key = self._KEY_MAP.get(key, key.upper())
                # Convert percentage values to decimals for config.py compatibility
                value = self._convert_value(snake_key, value)
                self._overrides[snake_key] = value
            log(_SRC, f"Category '{category}' updated")
    
    def reset(self):
        """Clear all overrides, revert to config.py defaults."""
        self._overrides.clear()
        self._initialized = False
        log(_SRC, "Config reset to defaults")
    
    def is_initialized(self):
        """Check if config has been received from backend."""
        return self._initialized
    
    def get_override(self, name):
        """Get override value only (None if not overridden)."""
        return self._overrides.get(name)
    
    def has_override(self, name):
        """Check if a specific key has an override."""
        return name in self._overrides
    
    def to_dict(self):
        """Export current effective config as dict."""
        result = {}
        
        # Get all keys from config.py
        for key in dir(_defaults):
            if key.isupper() and not key.startswith('_'):
                result[key] = getattr(self, key)
        
        return result
    
    def debug_print(self):
        """Print current config state for debugging."""
        log(_SRC, "=== Current Configuration ===")
        log(_SRC, f"Initialized from backend: {self._initialized}")
        log(_SRC, f"Override count: {len(self._overrides)}")
        
        # Key settings
        log(_SRC, f"GLOBAL_MODE: {self.GLOBAL_MODE}")
        log(_SRC, f"AUTO_DIM_ENABLED: {self.AUTO_DIM_ENABLED}")
        log(_SRC, f"MIN_BRIGHTNESS: {self.MIN_BRIGHTNESS}")
        log(_SRC, f"MAX_BRIGHTNESS: {self.MAX_BRIGHTNESS}")
        log(_SRC, f"LUX_MIN: {self.LUX_MIN}")
        log(_SRC, f"LUX_MAX: {self.LUX_MAX}")


# Global singleton instance
cfg = RuntimeConfig()

