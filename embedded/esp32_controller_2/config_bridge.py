"""
Config Bridge - Uses provisioned WiFi config

WiFi credentials come from NVS (set via web provisioning).
MQTT and other settings come from config.py.
"""

import config as hardcoded_config

try:
    from config_manager import config_manager
    HAS_CONFIG_MANAGER = True
except ImportError:
    HAS_CONFIG_MANAGER = False


class ConfigBridge:
    """Bridges provisioned WiFi config with hardcoded config.py values."""
    
    def __init__(self):
        self._wifi_config = {}
        self._load_wifi_config()
    
    def _load_wifi_config(self):
        """Load provisioned WiFi config if available."""
        if HAS_CONFIG_MANAGER:
            try:
                self._wifi_config = config_manager.load()
            except Exception:
                pass
    
    def is_provisioned(self):
        """Check if device is provisioned with WiFi."""
        if HAS_CONFIG_MANAGER:
            return config_manager.is_provisioned()
        return False
    
    @property
    def WIFI_SSID(self):
        """Get WiFi SSID - from provisioning or hardcoded."""
        if self._wifi_config.get("provisioned"):
            return self._wifi_config.get("wifi_ssid", "")
        return getattr(hardcoded_config, "WIFI_SSID", "")
    
    @property
    def WIFI_PASSWORD(self):
        """Get WiFi password - from provisioning or hardcoded."""
        if self._wifi_config.get("provisioned"):
            return self._wifi_config.get("wifi_password", "")
        return getattr(hardcoded_config, "WIFI_PASSWORD", "")
    
    # All other config comes directly from config.py
    @property
    def MQTT_BROKER(self):
        return getattr(hardcoded_config, "MQTT_BROKER", "192.168.1.100")
    
    @property
    def MQTT_PORT(self):
        return getattr(hardcoded_config, "MQTT_PORT", 1883)
    
    @property
    def MQTT_USER(self):
        return getattr(hardcoded_config, "MQTT_USER", None)
    
    @property
    def MQTT_PASSWORD(self):
        return getattr(hardcoded_config, "MQTT_PASSWORD", None)
    
    @property
    def MQTT_CLIENT_ID(self):
        return getattr(hardcoded_config, "MQTT_CLIENT_ID", "esp32-smartlight")
    
    @property
    def MQTT_BASE_TOPIC(self):
        return getattr(hardcoded_config, "MQTT_BASE_TOPIC", "smartlight")
    
    def __getattr__(self, name):
        """Fall back to hardcoded config for any other attribute."""
        return getattr(hardcoded_config, name)


_config_bridge = None

def get_config():
    """Get the config bridge singleton."""
    global _config_bridge
    if _config_bridge is None:
        _config_bridge = ConfigBridge()
    return _config_bridge


def factory_reset():
    """Perform factory reset - clear provisioned WiFi config."""
    if HAS_CONFIG_MANAGER:
        config_manager.clear()
        return True
    return False
