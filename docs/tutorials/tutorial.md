# Smart Lighting Tutorial

This tutorial guides you through setting up and using the Smart Lighting system to control your home lighting through a web interface and IoT devices.

## Prerequisites

Before starting, ensure you have:

- **Docker** and **Docker Compose** installed
- **Java 21** (for backend development)
- **Node.js 18+** and npm (for frontend development)
- **Git** for version control

## 1. Quick Start

### Clone and Start

```bash
# Clone the repository
git clone https://github.com/your-org/smart-lighting-scenes.git
cd smart-lighting-scenes

# Start infrastructure (PostgreSQL, Redis, MQTT broker)
docker-compose -f infra/docker-compose.yml up -d

# Start backend (Terminal 1)
cd backend
./gradlew bootRun

# Start frontend (Terminal 2)
cd frontend
npm install
npm run dev
```

### Access the Application

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **Database Admin:** http://localhost:8090

## 2. Authentication

The system uses Google OAuth for authentication. Users are assigned roles that determine their access level.

### User Roles

| Role | Permissions |
|------|-------------|
| OWNER | Full access: manage users, settings, devices, rooms |
| RESIDENT | Control devices, create scenes, view all data |
| GUEST | View-only access to rooms and device states |

### Sign In

1. Navigate to http://localhost:5173
2. Click "Sign in with Google"
3. Authorize the application
4. You'll be redirected to the dashboard

## 3. Managing Rooms

Rooms are logical groupings for your lighting devices.

### Create a Room via API

```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<your-session-id>" \
  -d '{
    "name": "Living Room",
    "description": "Main living area"
  }'
```

### List All Rooms

```bash
curl http://localhost:8080/api/rooms \
  -H "Cookie: JSESSIONID=<your-session-id>"
```

Response:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Living Room",
    "description": "Main living area",
    "devices": []
  }
]
```

## 4. Adding Devices

Devices represent physical lighting fixtures controlled via MQTT.

### Create a Light Device

```bash
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<your-session-id>" \
  -d '{
    "name": "Ceiling Light",
    "roomId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "LIGHT",
    "mqttCmdTopic": "smartlighting/command/esp32-001/led/0",
    "mqttStateTopic": "smartlighting/status/esp32-001/led/0"
  }'
```

### Device Types

| Type | Description |
|------|-------------|
| LIGHT | RGB or white light fixture |
| SENSOR | Environmental sensor (temperature, motion) |
| SWITCH | Physical wall switch or relay |

## 5. Controlling Lights

### Turn On a Light

```bash
curl -X PUT http://localhost:8080/api/devices/{deviceId}/state \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=<your-session-id>" \
  -d '{
    "isOn": true,
    "brightnessPct": 80,
    "rgbColor": "#FF9500"
  }'
```

### Device State Properties

| Property | Type | Description |
|----------|------|-------------|
| isOn | boolean | Power state |
| brightnessPct | integer | Brightness (0-100) |
| colorTempMired | integer | Color temperature in mireds |
| rgbColor | string | Hex color code (#RRGGBB) |

## 6. MQTT Integration

The system communicates with ESP32 controllers via MQTT.

### Topic Structure

```
smartlighting/
├── command/
│   └── {controllerId}/
│       ├── led/{index}     # Individual LED control
│       ├── scene           # Scene activation
│       └── global          # All LEDs
└── status/
    └── {controllerId}/     # Controller status updates
```

### LED Command Format

```json
{
  "rgb": [255, 149, 0],
  "brightness": 80,
  "on": true
}
```

### Scene Command Format

```json
{
  "sceneName": "evening"
}
```

## 7. Frontend Development

### Project Structure

```
frontend/
├── src/
│   ├── api/          # API client modules
│   ├── components/   # Vue components
│   ├── stores/       # Pinia state management
│   ├── utils/        # Utilities (logger, guards)
│   └── views/        # Page components
```

### Run Tests

```bash
cd frontend
npm run test           # Run all tests
npm run test:watch     # Watch mode
npm run test:coverage  # With coverage
```

### Run Linting

```bash
npm run lint           # Fix issues
npm run lint:check     # Check only
npm run format         # Format with Prettier
```

## 8. Backend Development

### Project Structure

```
backend/
├── src/main/java/.../
│   ├── controller/   # REST endpoints
│   ├── service/      # Business logic
│   ├── entity/       # JPA entities
│   ├── repository/   # Data access
│   ├── dto/          # Data transfer objects
│   ├── config/       # Configuration
│   └── security/     # OAuth2 setup
```

### Run Tests

```bash
cd backend
./gradlew test
```

### Generate Documentation

```bash
./gradlew docs
# Open: build/docs/javadoc/index.html
```

### Static Analysis

```bash
./gradlew analyze      # Run all checks
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew spotbugsMain
```

## 9. ESP32 Controller Setup

For hardware integration, flash the ESP32 with MicroPython firmware.

### Configuration

Edit `embedded/test-standalone/esp32_controller/config.py`:

```python
WIFI_SSID = "YourNetwork"
WIFI_PASSWORD = "YourPassword"

MQTT_BROKER = "192.168.1.100"
MQTT_PORT = 1883
MQTT_CLIENT_ID = "esp32-001"

LED_PIN = 13
LED_COUNT = 5
```

### LED to Room Mapping

| LED Index | Room |
|-----------|------|
| 0 | Living Room |
| 1 | Bedroom |
| 2 | Kitchen |
| 3 | Bathroom |
| 4 | Hallway |

## 10. Troubleshooting

### Backend won't start

Check if PostgreSQL is running:
```bash
docker-compose -f infra/docker-compose.yml ps
```

### Authentication fails

Ensure Google OAuth credentials are configured in `backend/src/main/resources/application.yml`:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
```

### MQTT not connecting

Verify the broker is running:
```bash
docker logs mosquitto
```

Test connectivity:
```bash
mosquitto_pub -h localhost -t "test" -m "hello"
mosquitto_sub -h localhost -t "test"
```

## Next Steps

- Explore the [API Documentation](../../backend/build/docs/javadoc/index.html)
- Review the [System Architecture](../SYSTEM_ARCHITECTURE.md)
- Check the [Embedded Design](../EMBEDDED_DESIGN_SUMMARY.md) for hardware details

