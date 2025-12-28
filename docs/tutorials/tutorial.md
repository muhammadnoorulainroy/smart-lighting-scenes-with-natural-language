# Smart Lighting Tutorial

This tutorial guides you through setting up and using the Smart Lighting system to control your home lighting through natural language commands, voice input, and intelligent automations.

## Prerequisites

Before starting, ensure you have:

- **Docker Desktop** installed and running
- **Git** for version control
- **Google Cloud Console** account (for OAuth)
- **OpenAI API key** (optional, for natural language processing)

For local development (optional):
- **Java 21** (JDK)
- **Node.js 18+** and npm

## 1. Quick Start

### Option A: Docker Deployment (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-org/smart-lighting-scenes.git
cd smart-lighting-scenes

# Create .env file with your credentials
cat > .env << EOF
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=your-jwt-secret
OPENAI_API_KEY=sk-your-openai-api-key
EOF

# Build and start the full stack
docker-compose build
docker-compose up -d
```

**Access the Application:**
- **Web Application:** http://localhost
- **Backend API:** http://localhost:8080

**Google OAuth Setup:**
Add this redirect URI in Google Cloud Console:
```
http://localhost/login/oauth2/code/google
```

### Option B: Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/smart-lighting-scenes.git
cd smart-lighting-scenes

# Create .env file (same as above)

# Start infrastructure only (PostgreSQL, Redis, MQTT broker)
docker-compose -f infra/docker-compose.yml up -d

# Start backend (Terminal 1)
cd backend
./gradlew bootRun

# Start frontend (Terminal 2)
cd frontend
npm install
npm run dev
```

**Access the Application:**
- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **Database Admin:** http://localhost:8090

**Google OAuth Setup:**
Add this redirect URI in Google Cloud Console:
```
http://localhost:8080/login/oauth2/code/google
```

## 2. Authentication

The system uses Google OAuth for authentication. Users are assigned roles that determine their access level.

### User Roles

| Role | Permissions |
|------|-------------|
| OWNER | Full access: manage users, settings, devices, rooms, scenes, schedules |
| RESIDENT | Control devices, create scenes, view all data |
| GUEST | View-only access to rooms and device states |

### Sign In

1. Navigate to http://localhost:5173
2. Click "Sign in with Google"
3. Authorize the application
4. You'll be redirected to the dashboard
5. First user becomes OWNER automatically

## 3. Natural Language Commands

The system understands natural language through OpenAI integration.

### Voice Input

1. Navigate to **Scenes** or **Dashboard**
2. Click the **🎤 microphone** icon
3. Speak your command
4. Review the **preview** showing what will happen
5. Click **Confirm** to execute

### Text Commands

Type commands in the natural language input field:

```
"Turn on the living room lights"
"Dim bedroom to 30%"
"Set kitchen lights to warm orange at 50%"
"Apply movie scene to all rooms"
"Turn off all lights"
```

### Scene Commands

```
"Apply relax scene to bedroom"
"Create a cozy scene with warm dim lights"
"Apply work scene"
```

### Schedule Commands

```
"Turn on porch light at sunset"
"Dim bedroom to 20% at 10pm every night"
"Apply morning scene at 7am on weekdays"
```

### Preview Before Execution

Every command shows a preview:

```json
{
  "understood": true,
  "commandType": "immediate",
  "target": "living_room",
  "action": {
    "on": true,
    "brightness": 75,
    "colorTemp": "warm"
  },
  "explanation": "This will turn on the living room lights at 75% brightness with warm color"
}
```

You can review and confirm before the command executes.

## 4. Managing Rooms

Rooms are logical groupings for your lighting devices.

### Create a Room (Web UI)

1. Go to **Dashboard**
2. Click **+ Add Room**
3. Enter room name and description
4. Click **Save**

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

## 5. Creating Scenes

Scenes are saved lighting configurations you can apply instantly.

### Preset Scenes

The system includes preset scenes:
- **Relax** - Warm, dim lights
- **Focus** - Bright, cool lights
- **Movie** - Very dim, warm lights
- **Party** - Colorful, dynamic lights
- **Sleep** - Minimal, red-tinted lights

### Create Custom Scene

1. Go to **Scenes**
2. Click **+ Create Scene**
3. Configure:
   - Name
   - Target room (or "All rooms")
   - Brightness
   - Color temperature or RGB
4. Click **Save**

### Apply Scene

- Click on a scene card, or
- Use voice: "Apply movie scene to bedroom"

### Scene Target Display

Each scene shows its target:
- 🏠 **All rooms**
- 🛏️ **Bedroom**
- 🛋️ **Living Room**
- etc.

## 6. Scheduling

Create automated lighting schedules.

### Create Schedule (Web UI)

1. Go to **Schedules**
2. Click **+ Create Schedule**
3. Configure:
   - Name
   - Action (scene or light settings)
   - Time/cron expression
   - Target room
4. Click **Save**

### Create Schedule with Voice

```
"Apply morning scene at 7am on weekdays"
"Turn on porch light at sunset"
"Dim all lights to 20% at 11pm"
```

### Schedule Conflict Detection

When creating a schedule that conflicts with existing ones:

1. The system detects the conflict
2. Shows affected schedules
3. Provides resolution options:
   - Adjust time
   - Disable conflicting schedule
   - Merge schedules
4. Select a resolution and apply

## 7. System Settings (Owner Only)

Configure ESP32 behavior from the web dashboard.

### Access Settings

1. Click your profile menu (top right)
2. Select **Settings**

### Lighting Settings

| Setting | Description |
|---------|-------------|
| Global Mode | Auto (sensor-based) or Manual |
| Auto Dim | Enable lux-based brightness adjustment |
| Sensor Override | Allow sensors to adjust scene values |
| Min/Max Brightness | Brightness limits (0-100%) |
| Lux Thresholds | Dark/bright room detection |

### Climate Settings

| Setting | Description |
|---------|-------------|
| Temperature Range | Color warmth adjustment thresholds |
| Blend Strength | How much temperature affects color |
| Humidity Range | Saturation adjustment thresholds |

### Audio Settings

| Setting | Description |
|---------|-------------|
| Disco Mode | Enable sound-reactive effects |
| Audio Threshold | Sensitivity for disco trigger |
| Flash Brightness | Brightness during disco |

### Display Settings

| Setting | Description |
|---------|-------------|
| OLED Auto-Sleep | Power save for ESP32 display |
| Show Time | Display clock on home screen |
| Show Sensor Data | Display readings on pages |

### Sync to Devices

After changing settings:
- Click **Save All Changes** to save and sync
- Or click **Sync to Devices** to push current settings

## 8. Mode Control

### Auto Mode (Default)

Sensors actively adjust lighting:
- Temperature affects color warmth
- Humidity affects saturation
- Ambient light affects brightness
- Scenes set base values that sensors modify

### Manual Mode

Sensors do not adjust lighting:
- User/scene settings applied exactly
- Useful for specific lighting needs

### Toggle Mode

From the **Rooms** page:
- Click the **Mode** button on a room
- Or use voice: "Set mode to manual"

## 9. MQTT Integration

The system communicates with ESP32 controllers via MQTT.

### Topic Structure

```
smartlighting/
├── command/
│   ├── lights              # "on", "off", "toggle"
│   ├── mode                # "auto", "manual"
│   └── scene               # {"sceneName":"relax","target":"bedroom"}
├── led/{index}/
│   ├── power               # "on", "off"
│   ├── brightness          # 0-100
│   └── color               # {"r":255,"g":100,"b":50}
├── room/{name}/
│   └── power, brightness
├── config/
│   └── lighting, climate, audio, display
└── status/
    └── online, led/state, sensor/data
```

### Test Commands

```bash
# Turn on all lights
mosquitto_pub -h localhost -t "smartlighting/command/lights" -m "on"

# Set LED 0 color
mosquitto_pub -h localhost -t "smartlighting/led/0/color" -m '{"r":255,"g":0,"b":0}'

# Apply scene
mosquitto_pub -h localhost -t "smartlighting/scene/apply" -m '{"sceneName":"relax"}'
```

## 10. ESP32 Hardware Setup

For hardware integration, see [embedded-setup.md](embedded-setup.md).

### Quick Overview

| Component | Role |
|-----------|------|
| ESP32 #1 | BLE sensor hub |
| ESP32 #2 | Main controller (LEDs, OLED, WiFi) |
| nRF52840 x2 | Environmental sensors |
| WS2812B LEDs | Room lighting (5 LEDs) |

### LED to Room Mapping

| LED Index | Room |
|-----------|------|
| 0 | Living Room |
| 1 | Bedroom |
| 2 | Kitchen |
| 3 | Bathroom |
| 4 | Hallway |

### WiFi Provisioning

If ESP32 can't connect to WiFi:
1. Hold Button A for 5 seconds during boot
2. Connect to "SmartLight-Setup" WiFi
3. Navigate to 192.168.4.1
4. Enter WiFi credentials
5. ESP32 reboots and connects

## 11. Frontend Development

### Project Structure

```
frontend/src/
├── api/              # API client modules
├── components/       # Reusable Vue components
├── views/            # Page components
├── stores/           # Pinia state management
└── utils/            # Utilities
```

### Run Development Server

```bash
cd frontend
npm install
npm run dev
```

### Run Tests

```bash
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

## 12. Backend Development

### Project Structure

```
backend/src/main/java/.../
├── controller/       # REST endpoints
├── service/          # Business logic
├── entity/           # JPA entities
├── repository/       # Data access
├── dto/              # Data transfer objects
├── config/           # Configuration
└── security/         # OAuth2 setup
```

### Run Development Server

```bash
cd backend
./gradlew bootRun
```

### Run Tests

```bash
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

## 13. Troubleshooting

### Backend won't start

Check if PostgreSQL is running:
```bash
docker-compose -f infra/docker-compose.yml ps
```

### Authentication fails

Ensure Google OAuth credentials are configured in `.env`:
```
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

### NLP commands not working

Verify OpenAI API key in `.env`:
```
OPENAI_API_KEY=sk-your-api-key
```

Check backend logs for API errors.

### Settings not applied to ESP32

1. Verify MQTT broker is running
2. Check ESP32 serial logs for config messages
3. Try "Sync to Devices" button
4. Verify ESP32 subscribes to `smartlighting/config/#`

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

### Schedule conflicts

Use the conflict resolution UI in Schedules view:
1. Click on conflicting schedule
2. Review suggested resolutions
3. Select and apply a resolution

## Next Steps

- Explore the [API Documentation](../API.md)
- Review the [Embedded System Documentation](../EMBEDDED_SYSTEM.md)
- Check the [Development Guide](../DEVELOPMENT.md)
- Set up [ESP32 Hardware](embedded-setup.md)
