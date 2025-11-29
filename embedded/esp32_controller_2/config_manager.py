"""
Configuration Manager - NVS-based persistent storage
Stores WiFi credentials in ESP32 flash memory.
MQTT and other settings come from config.py
"""

import json

try:
    import esp32
    HAS_NVS = True
except ImportError:
    HAS_NVS = False
    print("[WARN] esp32.NVS not available - using file-based config")


class ConfigManager:
    """Manages persistent WiFi configuration storage."""
    
    DEFAULT_CONFIG = {
        "wifi_ssid": "",
        "wifi_password": "",
        "provisioned": False,
    }
    
    NVS_NAMESPACE = "smartlight"
    NVS_KEY = "wifi"
    FILE_PATH = "wifi_config.json"
    
    def __init__(self):
        self._config = None
    
    def load(self):
        """Load configuration from NVS or file."""
        if self._config is not None:
            return self._config
        
        config = None
        
        if HAS_NVS:
            try:
                nvs = esp32.NVS(self.NVS_NAMESPACE)
                buf = bytearray(512)
                length = nvs.get_blob(self.NVS_KEY, buf)
                if length and length > 0:
                    data = buf[:length].decode('utf-8')
                    config = json.loads(data)
                    print("[CONFIG] Loaded from NVS")
            except Exception as e:
                print("[CONFIG] NVS load failed:", e)
        
        if config is None:
            try:
                with open(self.FILE_PATH, 'r') as f:
                    config = json.loads(f.read())
                    print("[CONFIG] Loaded from file")
            except Exception:
                pass
        
        if config is None:
            config = {}
        
        # Merge with defaults
        result = {}
        for k, v in self.DEFAULT_CONFIG.items():
            result[k] = v
        for k, v in config.items():
            result[k] = v
        
        self._config = result
        return self._config
    
    def save(self, config=None):
        """Save configuration to NVS and file."""
        if config is not None:
            # Merge with defaults
            result = {}
            for k, v in self.DEFAULT_CONFIG.items():
                result[k] = v
            for k, v in config.items():
                result[k] = v
            self._config = result
        
        if self._config is None:
            return False
        
        data = json.dumps(self._config)
        success = False
        
        if HAS_NVS:
            try:
                nvs = esp32.NVS(self.NVS_NAMESPACE)
                nvs.set_blob(self.NVS_KEY, data.encode('utf-8'))
                nvs.commit()
                print("[CONFIG] Saved to NVS")
                success = True
            except Exception as e:
                print("[CONFIG] NVS save failed:", e)
        
        try:
            with open(self.FILE_PATH, 'w') as f:
                f.write(data)
            print("[CONFIG] Saved to file")
            success = True
        except Exception as e:
            print("[CONFIG] File save failed:", e)
        
        return success
    
    def get(self, key, default=None):
        """Get a configuration value."""
        config = self.load()
        return config.get(key, default)
    
    def is_provisioned(self):
        """Check if device has been provisioned with WiFi credentials."""
        config = self.load()
        return bool(config.get("provisioned")) and bool(config.get("wifi_ssid"))
    
    def get_wifi_credentials(self):
        """Get WiFi SSID and password."""
        config = self.load()
        return config.get("wifi_ssid", ""), config.get("wifi_password", "")
    
    def clear(self):
        """Clear all configuration (factory reset)."""
        self._config = {}
        for k, v in self.DEFAULT_CONFIG.items():
            self._config[k] = v
        
        if HAS_NVS:
            try:
                nvs = esp32.NVS(self.NVS_NAMESPACE)
                nvs.erase_key(self.NVS_KEY)
                nvs.commit()
                print("[CONFIG] NVS cleared")
            except Exception:
                pass
        
        try:
            import os
            os.remove(self.FILE_PATH)
            print("[CONFIG] File removed")
        except Exception:
            pass
        
        return True


config_manager = ConfigManager()
