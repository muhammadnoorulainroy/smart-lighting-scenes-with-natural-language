"""
WiFi Provisioning - SoftAP Captive Portal (WiFi-Only)
Creates a WiFi access point with a web interface for WiFi configuration.

User Flow:
1. ESP32 creates AP: "SmartLight-Setup-XXXX"
2. User connects phone/laptop to that network (password: smartlight)
3. User opens browser -> 192.168.4.1
4. User enters WiFi credentials
5. ESP32 tests credentials - shows error if wrong
6. On success: saves config and reboots
"""

import network
import socket
import time
import gc
import machine

from config_manager import config_manager


def get_device_id():
    """Get unique device ID from MAC address."""
    import ubinascii
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    mac = wlan.config('mac')
    wlan.active(False)
    return ubinascii.hexlify(mac[-2:]).decode().upper()


def url_decode(s):
    """Decode URL-encoded string."""
    result = []
    i = 0
    while i < len(s):
        if s[i] == '%' and i + 2 < len(s):
            try:
                result.append(chr(int(s[i+1:i+3], 16)))
                i += 3
                continue
            except ValueError:
                pass
        elif s[i] == '+':
            result.append(' ')
            i += 1
            continue
        result.append(s[i])
        i += 1
    return ''.join(result)


def parse_form_data(body):
    """Parse URL-encoded form data."""
    data = {}
    if not body:
        return data
    for pair in body.split('&'):
        if '=' in pair:
            key, value = pair.split('=', 1)
            data[url_decode(key)] = url_decode(value)
    return data


def scan_networks(ap_if=None):
    """Scan for available WiFi networks."""
    try:
        wlan = network.WLAN(network.STA_IF)
        wlan.active(True)
        time.sleep(0.3)
        networks = wlan.scan()
        wlan.active(False)
        
        result = []
        seen = set()
        
        for net in networks:
            ssid = net[0].decode('utf-8', 'ignore')
            rssi = net[3]
            if ssid and ssid not in seen:
                seen.add(ssid)
                result.append((ssid, rssi))
        
        result.sort(key=lambda x: x[1], reverse=True)
        return result[:15]
    except Exception as e:
        print("[PROV] Scan error:", e)
        return []


def get_testing_page(ssid):
    """Generate a page shown during WiFi testing with auto-redirect."""
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="12;url=/">
    <title>Testing WiFi...</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
        }
        .container { text-align: center; padding: 40px; }
        .spinner {
            width: 60px; height: 60px;
            border: 4px solid rgba(255,255,255,0.2);
            border-top-color: #4CAF50;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 0 auto 20px;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        h2 { margin-bottom: 10px; }
        p { color: #aaa; margin-bottom: 10px; }
        .hint { font-size: 12px; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="spinner"></div>
        <h2>Testing WiFi Connection</h2>
        <p>Connecting to: <strong>""" + ssid + """</strong></p>
        <p>Please wait... This may take up to 15 seconds.</p>
        <p class="hint">Your phone will briefly disconnect during testing.<br>
        Reconnect to SmartLight-Setup to see the result.</p>
    </div>
</body>
</html>"""


def get_html_page(networks, message="", success=False):
    """Generate the WiFi configuration HTML page."""
    
    network_options = '<option value="">-- Select a network --</option>\n'
    for ssid, rssi in networks:
        bars = "████" if rssi > -50 else "███░" if rssi > -65 else "██░░" if rssi > -75 else "█░░░"
        network_options += '<option value="{}">{} ({}dBm) {}</option>\n'.format(
            ssid, ssid, rssi, bars
        )
    
    if len(networks) == 0:
        network_options = '<option value="">No networks found - click Refresh</option>'
    
    message_html = ""
    if message:
        color = "#4CAF50" if success else "#f44336"
        message_html = '<div class="message" style="background:{};">{}</div>'.format(color, message)
    
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SmartLight WiFi Setup</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            padding: 20px;
            color: #fff;
        }
        .container {
            max-width: 400px;
            margin: 0 auto;
            background: rgba(255,255,255,0.1);
            border-radius: 16px;
            padding: 30px;
            backdrop-filter: blur(10px);
        }
        h1 { text-align: center; margin-bottom: 10px; font-size: 24px; }
        .subtitle { text-align: center; color: #aaa; margin-bottom: 25px; font-size: 14px; }
        .section { margin-bottom: 20px; }
        .section-title {
            font-size: 14px; color: #888; text-transform: uppercase;
            margin-bottom: 12px; letter-spacing: 1px;
        }
        label { display: block; margin-bottom: 6px; font-size: 14px; color: #ccc; }
        label .required { color: #ff6b6b; }
        input, select {
            width: 100%; padding: 12px;
            border: 1px solid rgba(255,255,255,0.2); border-radius: 8px;
            background: rgba(0,0,0,0.3); color: #fff; font-size: 16px; margin-bottom: 12px;
        }
        input:focus, select:focus { outline: none; border-color: #4CAF50; }
        input.error, select.error { border-color: #ff6b6b; }
        input::placeholder { color: #666; }
        .btn {
            width: 100%; padding: 14px; color: white; border: none;
            border-radius: 8px; font-size: 16px; font-weight: bold; cursor: pointer;
            text-decoration: none; display: block; text-align: center;
        }
        .btn:hover { opacity: 0.9; }
        .btn:active { transform: scale(0.98); }
        .btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .btn-primary { background: linear-gradient(135deg, #4CAF50, #45a049); margin-top: 10px; }
        .btn-secondary { background: linear-gradient(135deg, #2196F3, #1976D2); }
        .network-row { display: flex; gap: 10px; margin-bottom: 12px; }
        .network-row select { flex: 1; margin-bottom: 0; }
        .network-row .btn { width: auto; padding: 12px 16px; margin-top: 0; }
        .message { color: white; padding: 12px; border-radius: 8px; margin-bottom: 20px; text-align: center; }
        .error-msg { color: #ff6b6b; font-size: 12px; margin-top: -8px; margin-bottom: 8px; display: none; }
        .hint { font-size: 12px; color: #888; margin-top: 20px; text-align: center; }
    </style>
</head>
<body>
    <div class="container">
        <h1>SmartLight WiFi Setup</h1>
        <p class="subtitle">Connect your device to WiFi</p>
        
        """ + message_html + """
        
        <form id="configForm" action="/connect" method="POST" onsubmit="return validateForm()">
            <div class="section">
                <div class="section-title">WiFi Network</div>
                
                <label>Select Network <span class="required">*</span></label>
                <div class="network-row">
                    <select name="wifi_ssid" id="wifi_ssid" required>
                        """ + network_options + """
                    </select>
                    <a href="/scan" class="btn btn-secondary">Refresh</a>
                </div>
                <p id="ssid_error" class="error-msg">Please select a WiFi network</p>
                
                <label>WiFi Password <span class="required">*</span></label>
                <input type="password" name="wifi_password" id="wifi_password" 
                       placeholder="Enter WiFi password" required minlength="8">
                <p id="pass_error" class="error-msg">Password must be at least 8 characters</p>
            </div>
            
            <button type="submit" class="btn btn-primary" id="submitBtn">Connect</button>
        </form>
        
        <p class="hint">MQTT and other settings are configured in the device firmware.</p>
    </div>
    
    <script>
        function validateForm() {
            var valid = true;
            
            document.querySelectorAll('.error').forEach(function(el) { el.classList.remove('error'); });
            document.querySelectorAll('.error-msg').forEach(function(el) { el.style.display = 'none'; });
            
            var ssid = document.getElementById('wifi_ssid');
            if (!ssid.value || ssid.value === '') {
                ssid.classList.add('error');
                document.getElementById('ssid_error').style.display = 'block';
                valid = false;
            }
            
            var pass = document.getElementById('wifi_password');
            if (!pass.value || pass.value.length < 8) {
                pass.classList.add('error');
                document.getElementById('pass_error').style.display = 'block';
                valid = false;
            }
            
            if (valid) {
                document.getElementById('submitBtn').disabled = true;
                document.getElementById('submitBtn').textContent = 'Connecting...';
            }
            
            return valid;
        }
    </script>
</body>
</html>"""


def get_success_page():
    """Generate success page after WiFi connection verified."""
    return """<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WiFi Connected</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
        }
        .container { text-align: center; padding: 40px; }
        .check {
            width: 80px; height: 80px;
            background: #4CAF50; border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            margin: 0 auto 20px; font-size: 40px;
        }
        h1 { margin-bottom: 10px; }
        p { color: #aaa; margin-bottom: 10px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="check">✓</div>
        <h1>WiFi Connected!</h1>
        <p>Device is rebooting to start normal operation.</p>
        <p style="font-size:14px">You can close this page and reconnect to your normal WiFi.</p>
    </div>
</body>
</html>"""


class WiFiProvisioning:
    """WiFi provisioning via SoftAP captive portal."""
    
    def __init__(self, oled=None):
        self.oled = oled
        self.ap = None
        self.server = None
        self.device_id = get_device_id()
        self.ap_ssid = "SmartLight-Setup-{}".format(self.device_id)
        self.ap_password = "smartlight"
        self.last_error = ""  # Store error from failed WiFi test
    
    def show_on_oled(self, line1, line2="", line3="", line4=""):
        """Display message on OLED if available."""
        if self.oled is None:
            return
        try:
            self.oled.fill(0)
            self.oled.text(line1, 0, 0)
            if line2:
                self.oled.text(line2, 0, 16)
            if line3:
                self.oled.text(line3, 0, 32)
            if line4:
                self.oled.text(line4, 0, 48)
            self.oled.show()
        except Exception:
            pass
    
    def start_ap(self):
        """Start the access point."""
        print("[PROV] Starting AP: {}".format(self.ap_ssid))
        
        sta = network.WLAN(network.STA_IF)
        sta.active(False)
        time.sleep(0.2)
        
        self.ap = network.WLAN(network.AP_IF)
        self.ap.active(True)
        time.sleep(0.5)
        
        self.ap.config(
            essid=self.ap_ssid,
            password=self.ap_password,
            authmode=network.AUTH_WPA_WPA2_PSK
        )
        
        timeout = 10
        while not self.ap.active() and timeout > 0:
            time.sleep(0.5)
            timeout -= 1
        
        if not self.ap.active():
            print("[PROV] ERROR: Failed to start AP!")
            return None
        
        try:
            self.ap.ifconfig(('192.168.4.1', '255.255.255.0', '192.168.4.1', '192.168.4.1'))
        except Exception:
            pass
        
        ip = self.ap.ifconfig()[0]
        print("[PROV] AP active: {} / {}".format(self.ap_ssid, ip))
        
        self.show_on_oled(
            "WiFi Setup",
            self.ap_ssid,
            "Pass: " + self.ap_password,
            "Open 192.168.4.1"
        )
        
        return ip
    
    def stop_ap(self):
        """Stop the access point."""
        if self.ap:
            self.ap.active(False)
            self.ap = None
        print("[PROV] AP stopped")
    
    def test_wifi_connection(self, ssid, password, timeout=15):
        """Test WiFi credentials by actually connecting."""
        print("[PROV] Testing WiFi: {}".format(ssid))
        self.show_on_oled("Testing WiFi...", ssid, "", "Please wait")
        
        # Temporarily disable AP and try STA connection
        if self.ap:
            self.ap.active(False)
        time.sleep(0.5)
        
        sta = network.WLAN(network.STA_IF)
        sta.active(True)
        time.sleep(0.5)
        
        connected = False
        result_ip = ""
        error_msg = ""
        
        try:
            sta.connect(ssid, password)
            
            start = time.time()
            while not sta.isconnected() and (time.time() - start) < timeout:
                time.sleep(0.5)
                print("[PROV] Connecting... {:.0f}s".format(time.time() - start))
            
            if sta.isconnected():
                result_ip = sta.ifconfig()[0]
                print("[PROV] WiFi connected! IP: {}".format(result_ip))
                connected = True
            else:
                print("[PROV] WiFi connection failed - timeout")
                error_msg = "Connection timed out - check password"
                sta.disconnect()
                sta.active(False)
        
        except Exception as e:
            print("[PROV] WiFi error:", e)
            error_msg = str(e)
            try:
                sta.disconnect()
                sta.active(False)
            except:
                pass
        
        # Handle result
        if connected:
            # Keep WiFi connected, but also re-enable AP to show success page
            self.show_on_oled("WiFi OK!", "IP: " + result_ip, "", "Saving...")
            return True, result_ip
        else:
            # Re-enable AP for retry
            sta.active(False)
            time.sleep(0.3)
            self.ap = network.WLAN(network.AP_IF)
            self.ap.active(True)
            self.ap.config(
                essid=self.ap_ssid,
                password=self.ap_password,
                authmode=network.AUTH_WPA_WPA2_PSK
            )
            time.sleep(0.5)
            self.show_on_oled(
                "WiFi FAILED!",
                "Reconnect to:",
                self.ap_ssid,
                "and try again"
            )
            return False, error_msg
    
    def run_server(self):
        """Run the captive portal web server."""
        addr = socket.getaddrinfo('0.0.0.0', 80)[0][-1]
        
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server.bind(addr)
        self.server.listen(3)
        self.server.setblocking(False)
        
        print("[PROV] Web server started on port 80")
        
        networks = []
        networks_scanned = False
        
        while True:
            try:
                gc.collect()
                
                client = None
                try:
                    client, client_addr = self.server.accept()
                except OSError as e:
                    if e.args[0] in (11, 110, 35):
                        time.sleep(0.1)
                        continue
                    else:
                        time.sleep(0.5)
                        continue
                
                if client is None:
                    continue
                
                client.setblocking(True)
                client.settimeout(10.0)
                
                try:
                    request = b""
                    while True:
                        chunk = client.recv(1024)
                        if not chunk:
                            break
                        request += chunk
                        if b"\r\n\r\n" in request:
                            break
                    request = request.decode('utf-8', 'ignore')
                except Exception as e:
                    print("[PROV] Recv error:", e)
                    self._close_client(client)
                    continue
                
                if not request:
                    self._close_client(client)
                    continue
                
                lines = request.split('\r\n')
                first_line = lines[0] if lines else ""
                parts = first_line.split(' ')
                method = parts[0] if parts else ""
                path = parts[1] if len(parts) > 1 else "/"
                
                body = ""
                if '\r\n\r\n' in request:
                    body = request.split('\r\n\r\n', 1)[1]
                
                print("[PROV] {} {}".format(method, path))
                
                # Scan networks on first load or refresh
                if path == "/scan" or path == "/scan/" or not networks_scanned:
                    print("[PROV] Scanning networks...")
                    networks = scan_networks()
                    networks_scanned = True
                    if path.startswith("/scan"):
                        html = get_html_page(networks, "Networks refreshed", success=True)
                        self._send_response(client, html)
                        self._close_client(client)
                        continue
                
                # Handle WiFi connection attempt
                if path == "/connect" and method == "POST":
                    data = parse_form_data(body)
                    ssid = data.get("wifi_ssid", "").strip()
                    password = data.get("wifi_password", "")
                    
                    # Validate input
                    if not ssid:
                        html = get_html_page(networks, "Please select a WiFi network", success=False)
                        self._send_response(client, html)
                        self._close_client(client)
                        continue
                    
                    if len(password) < 8:
                        html = get_html_page(networks, "Password must be at least 8 characters", success=False)
                        self._send_response(client, html)
                        self._close_client(client)
                        continue
                    
                    # Show testing page (auto-refreshes after 20s)
                    self._send_response(client, get_testing_page(ssid))
                    self._close_client(client)
                    
                    # Test the WiFi connection
                    success, result = self.test_wifi_connection(ssid, password)
                    
                    if success:
                        # Save credentials
                        config = {
                            "wifi_ssid": ssid,
                            "wifi_password": password,
                            "provisioned": True,
                        }
                        config_manager.save(config)
                        print("[PROV] WiFi credentials saved!")
                        
                        # Re-enable AP briefly to show success page
                        print("[PROV] Re-enabling AP for success feedback...")
                        self.ap = network.WLAN(network.AP_IF)
                        self.ap.active(True)
                        self.ap.config(
                            essid=self.ap_ssid,
                            password=self.ap_password,
                            authmode=network.AUTH_WPA_WPA2_PSK
                        )
                        time.sleep(1)
                        
                        self.show_on_oled("WiFi OK!", "IP: " + result, "Rebooting in 5s", "")
                        
                        # Serve success page to any client that reconnects
                        self._serve_success_page_then_reboot(result)
                    else:
                        # Store error for next page load
                        self.last_error = "Connection failed: {}".format(result)
                        print("[PROV] WiFi test failed:", self.last_error)
                        # Re-scan networks for next page
                        networks = scan_networks()
                        # Server will handle next request with error message
                        # Note: user needs to refresh page manually after failed attempt
                        print("[PROV] WiFi test failed, waiting for retry...")
                        continue
                
                # Captive portal redirects
                elif path.startswith("/generate_204") or path.startswith("/hotspot-detect") or path == "/fwlink":
                    self._send_redirect(client)
                
                # Default: show form (with any stored error)
                else:
                    error_msg = self.last_error
                    self.last_error = ""  # Clear after showing
                    html = get_html_page(networks, error_msg, success=False if error_msg else True)
                    self._send_response(client, html)
                
                self._close_client(client)
                
            except KeyboardInterrupt:
                break
            except Exception as e:
                print("[PROV] Server error:", e)
                time.sleep(0.5)
        
        self.server.close()
    
    def _close_client(self, client):
        """Safely close client socket."""
        try:
            client.close()
        except Exception:
            pass
    
    def _send_response(self, client, html, status="200 OK"):
        """Send HTTP response."""
        try:
            html_bytes = html.encode('utf-8')
            header = "HTTP/1.1 {}\r\n".format(status)
            header += "Content-Type: text/html; charset=utf-8\r\n"
            header += "Content-Length: {}\r\n".format(len(html_bytes))
            header += "Connection: close\r\n"
            header += "Cache-Control: no-cache\r\n"
            header += "\r\n"
            
            client.sendall(header.encode('utf-8'))
            chunk_size = 512
            for i in range(0, len(html_bytes), chunk_size):
                client.sendall(html_bytes[i:i+chunk_size])
        except Exception as e:
            print("[PROV] Send error:", e)
    
    def _send_redirect(self, client):
        """Send redirect to captive portal."""
        try:
            response = "HTTP/1.1 302 Found\r\n"
            response += "Location: http://192.168.4.1/\r\n"
            response += "Connection: close\r\n"
            response += "\r\n"
            client.send(response.encode('utf-8'))
        except Exception:
            pass
    
    def _serve_success_page_then_reboot(self, ip_address):
        """Serve success page to reconnecting clients, then reboot."""
        print("[PROV] Waiting for client to show success page...")
        
        # Create a simple server to show success page
        try:
            success_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            success_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            success_server.bind(('0.0.0.0', 80))
            success_server.listen(2)
            success_server.settimeout(8)  # Wait up to 8 seconds for client
            
            try:
                client, addr = success_server.accept()
                print("[PROV] Client reconnected, showing success page")
                client.settimeout(5)
                
                # Read request (don't care about content)
                try:
                    client.recv(1024)
                except:
                    pass
                
                # Send success page
                self._send_response(client, get_success_page())
                self._close_client(client)
                print("[PROV] Success page sent!")
                
            except OSError:
                print("[PROV] No client reconnected (timeout)")
            
            success_server.close()
            
        except Exception as e:
            print("[PROV] Success server error:", e)
        
        # Show final OLED message and reboot
        self.show_on_oled("WiFi Saved!", "IP: " + ip_address, "", "Rebooting...")
        print("[PROV] Rebooting in 3 seconds...")
        time.sleep(3)
        machine.reset()
    
    def run(self):
        """Start provisioning mode."""
        print("\n" + "=" * 50)
        print("  SmartLight WiFi Provisioning")
        print("=" * 50 + "\n")
        
        self.start_ap()
        self.run_server()


def check_factory_reset_oled_button(pin_num, hold_seconds, oled=None):
    """Check for factory reset via OLED button hold on boot."""
    from machine import Pin
    
    btn = Pin(pin_num, Pin.IN, Pin.PULL_UP)
    
    # Button not pressed (pull-up means 1 = not pressed, 0 = pressed)
    if btn.value() == 1:
        return False
    
    print("[BOOT] Button A detected! Hold for {}s to factory reset WiFi config...".format(hold_seconds))
    
    if oled:
        try:
            oled.fill(0)
            oled.text("FACTORY RESET", 10, 0)
            oled.text("Hold button for", 0, 20)
            oled.text("{} seconds...".format(hold_seconds), 0, 35)
            oled.show()
        except:
            pass
    
    # Count how long button is held
    for i in range(hold_seconds):
        time.sleep(1)
        if btn.value() == 1:
            print("[BOOT] Button released early, normal boot")
            return False
        print("[BOOT] {} / {} seconds...".format(i + 1, hold_seconds))
        if oled:
            try:
                oled.fill(0)
                oled.text("FACTORY RESET", 10, 0)
                oled.text("Hold: {} / {}".format(i + 1, hold_seconds), 0, 25)
                oled.show()
            except:
                pass
    
    # Button held for full duration
    print("[BOOT] Factory reset triggered! Clearing WiFi config...")
    config_manager.clear()
    
    if oled:
        try:
            oled.fill(0)
            oled.text("RESET COMPLETE", 5, 20)
            oled.text("Rebooting...", 15, 40)
            oled.show()
        except:
            pass
    
    return True


def start_provisioning(oled=None):
    """Start WiFi provisioning mode."""
    prov = WiFiProvisioning(oled=oled)
    prov.run()
