# Smart Lighting Scenes with Natural Language

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-brightgreen.svg)](https://vuejs.org/)

A comprehensive smart home lighting control system that understands natural language commands and creates intelligent lighting scenes. Control your home lighting through voice commands, scheduled automations, and real-time IoT integration with environmental sensor adaptation.

---

## Quick Start with Docker

Recommended for trying out the application. Run the complete stack with Docker - no local Java or Node.js installation required.

### Prerequisites

- Docker Desktop installed and running
- Google OAuth credentials (for authentication)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd smart-lighting-scenes-with-natural-language
   ```

2. **Configure environment variables**
   
   Create a `.env` file in the project root:
   ```bash
   # Google OAuth (required)
   GOOGLE_CLIENT_ID=your-google-client-id
   GOOGLE_CLIENT_SECRET=your-google-client-secret

   # Database
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=postgres

   # JWT Secret
   JWT_SECRET=your-secure-jwt-secret

   # OpenAI API Key (optional, for NLP features)
   OPENAI_API_KEY=sk-your-openai-api-key
   ```

3. **Configure Google OAuth redirect URI**
   
   In Google Cloud Console, add this redirect URI:
   ```
   http://localhost/login/oauth2/code/google
   ```

4. **Build and start the application**
   ```bash
   docker-compose build
   docker-compose up -d
   ```

5. **Access the application**
   
   | Service | URL |
   |---------|-----|
   | Web Application | http://localhost |
   | Backend API | http://localhost:8080 |
   | Database | localhost:5433 |

### Docker Commands

```bash
# Start application
docker-compose up -d

# Stop application
docker-compose down

# View logs
docker-compose logs -f

# Rebuild after code changes
docker-compose build
docker-compose up -d

# Reset database (removes all data)
docker-compose down -v
docker-compose up -d
```

### Using Make (optional)

If GNU Make is installed:

```bash
make docker-build      # Build all images
make docker-up-app     # Start full stack
make docker-down-app   # Stop full stack
make docker-logs-app   # View logs
```

---

## Features

### Natural Language Processing
- **Voice & Text Commands**: Speak or type commands like "dim the living room lights to 30% warm" or "apply movie scene to bedroom at 8pm"
- **OpenAI Integration**: Powered by GPT for intelligent command parsing
- **Preview Before Execute**: See what will happen before confirming a command
- **Ambiguous Command Handling**: Clear error messages when commands need clarification

### Smart Scenes
- **Preset Scenes**: Pre-configured scenes (Relax, Focus, Movie, etc.)
- **Custom Scenes**: Create personalized lighting configurations
- **Room Targeting**: Apply scenes to specific rooms or all rooms
- **Scene Scheduling**: Schedule scenes for specific times with natural language

### Intelligent Scheduling
- **AI-Powered Conflict Detection**: Automatically detects overlapping schedules
- **Smart Resolution Suggestions**: Get realistic suggestions to resolve conflicts
- **Flexible Triggers**: Time-based, sunrise/sunset, and sensor-triggered schedules
- **Enable/Disable**: Toggle schedules without deleting them

### Environmental Adaptation
- **Temperature Color Shift**: Lights adjust color warmth based on room temperature
- **Humidity Saturation**: Color saturation adapts to humidity levels
- **Auto-Dimming**: Brightness automatically adjusts to ambient light (lux)
- **Disco Mode**: Sound-reactive lighting effects triggered by audio sensors
- **Sensor Override Toggle**: Enable/disable sensor adjustments per preference

### Runtime Configuration
- **Centralized Settings**: Configure all ESP32 behavior from the web dashboard
- **Live Updates via MQTT**: Settings sync to devices instantly
- **Categories**: Lighting, Climate, Audio, Display settings
- **Per-Setting Control**: Fine-tune min/max brightness, temperature ranges, thresholds

### Role-Based Access
- **Owner**: Full access to settings, users, devices, and rooms
- **Resident**: Control devices, create scenes, view all data
- **Guest**: View-only access

### Multi-Platform Support
- **Web Dashboard**: Vue 3 + Vite responsive web application
- **Android App**: Kotlin + Jetpack Compose (Material Design 3)
- **ESP32 Hardware**: Real-time LED control with sensor integration

## Architecture

This is a monorepo containing all components of the Smart Lighting system:

```
smart-lighting-scenes/
├── backend/                    # Spring Boot 3.x backend API
│   └── src/main/java/.../
│       ├── config/             # MqttConfig, SecurityConfig, WebSocketConfig
│       ├── controller/         # REST controllers
│       │   ├── AuthController.java
│       │   ├── ConfigController.java      # NEW: System settings
│       │   ├── NlpController.java         # NEW: NLP commands
│       │   ├── ScenesController.java
│       │   ├── SchedulesController.java
│       │   └── ...
│       ├── service/            # Business logic
│       │   ├── NlpService.java            # NEW: OpenAI NLP parsing
│       │   ├── ScheduleConflictService.java # NEW: AI conflict detection
│       │   ├── ConfigService.java         # NEW: Runtime config management
│       │   ├── MqttService.java
│       │   ├── SchedulerService.java
│       │   └── ...
│       ├── entity/             # JPA entities
│       │   ├── Scene.java, Schedule.java  # NEW
│       │   ├── NlpCommand.java            # NEW
│       │   ├── SystemConfig.java          # NEW
│       │   └── ...
│       └── dto/                # Data transfer objects
│           ├── NlpCommandDto.java         # NEW
│           ├── ConflictAnalysisDto.java   # NEW
│           └── ...
├── frontend/                   # Vue 3 + Vite web application
│   └── src/
│       ├── api/                # API clients
│       │   ├── nlp.js          # NEW: NLP API
│       │   ├── config.js       # NEW: Settings API
│       │   ├── scenes.js
│       │   ├── schedules.js
│       │   └── ...
│       ├── views/              # Page components
│       │   ├── SettingsView.vue    # NEW: System settings page
│       │   ├── ScenesView.vue      # With voice input
│       │   ├── SchedulesView.vue   # With conflict resolution
│       │   └── ...
│       └── components/         # Reusable components
├── embedded/                   # ESP32 MicroPython + nRF52840 CircuitPython
│   ├── esp32_controller_2/     # Main controller with sensors
│   │   ├── main.py             # Application entry point
│   │   ├── runtime_config.py   # NEW: Dynamic config from backend
│   │   ├── sensor_logic_async.py
│   │   ├── mqtt_client_async.py
│   │   ├── oled_display_async.py
│   │   └── ...
│   ├── esp32_controller_1/     # BLE sensor hub
│   └── nrf52840_sensor_*/      # Environmental sensors
├── mobile/                     # Android app (Kotlin + Jetpack Compose)
├── infra/                      # Docker Compose and infrastructure
└── docs/                       # Documentation
    ├── API.md                  # Complete API reference
    ├── EMBEDDED_SYSTEM.md      # Hardware documentation
    ├── DEVELOPMENT.md          # Development guide
    └── tutorials/              # Setup tutorials
```

## Local Development

For development with hot-reload and debugging capabilities.

### Prerequisites

- **Node.js 18+** and **npm 9+**
- **Java 21+** (JDK)
- **Docker** (for infrastructure services)
- **GNU Make** (optional)

```bash
# Verify tools
java --version    # 21+
node --version    # 18+
docker --version  # 20+
```

### Setup

**1. Configure environment**

Create a `.env` file in the project root:

```bash
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=your-jwt-secret
OPENAI_API_KEY=sk-your-api-key  # optional
```

**2. Add Google OAuth redirect URI**

In Google Cloud Console:
```
http://localhost:8080/login/oauth2/code/google
```

**3. Start infrastructure**

```bash
make docker-up
# Or: docker-compose -f infra/docker-compose.yml up -d
```

**4. Start backend** (Terminal 1)

```bash
make dev-backend
# Or: cd backend && ./gradlew bootRun
```

**5. Start frontend** (Terminal 2)

```bash
make dev-frontend
# Or: cd frontend && npm run dev
```

### Access

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Database Admin | http://localhost:8090 |

### Build

```bash
make build              # Build all
make build-backend      # Backend only
make build-frontend     # Frontend only
```

### Test

```bash
make test               # Run all tests
cd backend && ./gradlew test      # Backend tests
cd frontend && npm run test       # Frontend tests
```

---

## Quick Reference

| Task | Command | Description |
|------|---------|-------------|
| **Install** | `make install` | Install all dependencies |
| **Build** | `make build` | Build backend + frontend |
| **Test** | `make test` | Run all tests |
| **Run** | `make dev` | Show run instructions |
| **Clean** | `make clean` | Remove build artifacts |
| **Help** | `make help` | Show all available commands |

**Without Make:**
```bash
# Install
npm install && cd backend && ./gradlew build -x test && cd ../frontend && npm install

# Build
cd backend && ./gradlew build && cd ../frontend && npm run build

# Test  
cd backend && ./gradlew test && cd ../frontend && npm run test

# Run
docker-compose -f infra/docker-compose.yml up -d  # Infrastructure
cd backend && ./gradlew bootRun &                  # Backend
cd frontend && npm run dev                          # Frontend
```

---

## Natural Language Commands

The system understands various natural language commands:

### Immediate Commands
```
"Turn on the living room lights"
"Dim bedroom to 30%"
"Set kitchen lights to warm orange"
"Apply movie scene"
"All lights off"
```

### Scene Commands
```
"Apply relax scene to bedroom"
"Create a cozy scene with warm dim lights"
"Apply work scene to all rooms"
```

### Schedule Commands
```
"Turn on porch light at sunset"
"Dim bedroom to 20% at 10pm every night"
"Apply morning scene at 7am on weekdays"
```

### Voice Input
- Click the microphone icon in the Scenes or Dashboard view
- Speak your command
- Review the preview and confirm

---

## System Settings

Owners can configure ESP32 behavior from the Settings page:

### Lighting Settings
- **Global Mode**: Auto (sensor-based) or Manual
- **Auto-Dim**: Enable/disable lux-based brightness
- **Sensor Override**: Allow sensors to adjust scene values
- **Brightness Limits**: Min/Max brightness percentages

### Climate Settings
- **Temperature Range**: Color temperature adjustment thresholds
- **Blend Strength**: How much temperature affects color
- **Humidity Range**: Saturation adjustment thresholds

### Audio Settings
- **Disco Mode**: Enable/disable sound-reactive effects
- **Audio Threshold**: Sensitivity for disco trigger
- **Flash Brightness**: Brightness during disco mode

### Display Settings
- **OLED Auto-Sleep**: Power save for ESP32 display
- **Show Time**: Display clock on home screen
- **Show Sensor Data**: Display readings on detail pages

---

## MQTT Topics

The system uses the following MQTT topic structure:

```
smartlighting/
├── command/
│   ├── lights              # "on", "off", "toggle"
│   ├── mode                # "auto", "manual"
│   └── scene               # {"sceneName": "relax", "target": "bedroom"}
├── led/{index}/
│   ├── power               # "on", "off"
│   ├── brightness          # 0-100
│   └── color               # {"r":255,"g":100,"b":50}
├── room/{name}/
│   ├── power               # "on", "off"
│   └── brightness          # 0-100
├── config/
│   ├── lighting            # Lighting settings JSON
│   ├── climate             # Climate settings JSON
│   ├── audio               # Audio settings JSON
│   └── display             # Display settings JSON
└── status/
    ├── online              # Connection status (retained)
    ├── led/{index}/state   # LED state (retained)
    └── sensor/{name}       # Sensor readings
```

---

## Embedded System

### Hardware Components

| Component | Role | Connection |
|-----------|------|------------|
| **ESP32 #1** | BLE sensor hub | UART TX → ESP32 #2 |
| **ESP32 #2** | Main controller | WiFi/MQTT, LEDs, OLED |
| **nRF52840 x2** | Environmental sensors | BLE → ESP32 #1 |
| **WS2812B LEDs** | Room lighting (5 LEDs) | GPIO 13 |
| **SH1107 OLED** | Status display | I2C (GPIO 22/23) |

### LED to Room Mapping

| LED Index | Room | Sensor |
|-----------|------|--------|
| 0 | Living Room | SmartLight-Sensor-2 |
| 1 | Bedroom | SmartLight-Sensor-1 |
| 2 | Kitchen | - |
| 3 | Bathroom | - |
| 4 | Hallway | - |

### Runtime Configuration

The ESP32 receives configuration updates from the backend via MQTT:

```python
# runtime_config.py wraps config.py defaults with backend overrides
cfg = RuntimeConfig()

# Access settings (uses backend value if available, else default)
brightness = cfg.MAX_BRIGHTNESS  # From backend or config.py
```

Settings are automatically synced when changed in the web dashboard.

### WiFi Provisioning

If WiFi credentials aren't configured:
1. Hold Button A for 5 seconds during boot
2. Connect to "SmartLight-Setup" WiFi AP
3. Navigate to 192.168.4.1
4. Enter your WiFi credentials
5. ESP32 reboots and connects

---

## Database Schema

The PostgreSQL database includes tables for:
- **Users** (with OAuth integration and roles)
- **Rooms** and **Devices**
- **Scenes** (preset and custom with target rooms)
- **Schedules** (with actions and triggers)
- **NLP Commands** (command history)
- **System Config** (runtime settings)
- **Events** and **Sensor Readings**

See `backend/src/main/resources/db/migration/` for Flyway migrations.

---

## Development

### Code Style

- **Backend:** Spring Boot conventions, Lombok annotations, `@Slf4j` logging
- **Frontend:** ESLint + Prettier for Vue 3
- **Embedded:** MicroPython async/await patterns
- **Build:** Gradle (backend), npm + Vite (frontend), Make (orchestration)

### Logging Configuration

Backend logging is configured in `application.properties`:

```properties
logging.level.root=INFO
logging.level.com.example.smart.lighting=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Available Make Targets

```bash
make help          # Show all available commands
make install       # Install dependencies
make build         # Build all components
make test          # Run all tests
make clean         # Remove build artifacts
make docker-up     # Start infrastructure
make docker-down   # Stop infrastructure
make verify        # Complete verification (build + test + lint)
```

### npm Scripts (Root)

```bash
npm run dev:frontend    # Start Vue dev server
npm run dev:backend     # Start Spring Boot
npm run build:frontend  # Build Vue app
npm run build:backend   # Build Spring Boot JAR
npm run test            # Run all tests
npm run docker:up       # Start infrastructure
npm run docker:down     # Stop infrastructure
```

---

## Detailed Documentation

For comprehensive documentation, see:
- **[docs/API.md](docs/API.md)** - Complete API reference
- **[docs/EMBEDDED_SYSTEM.md](docs/EMBEDDED_SYSTEM.md)** - Hardware documentation
- **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Development guide

For comprehensive build automation documentation, see:
- **[BUILD.md](BUILD.md)** - Complete build system documentation
- **[docs/tutorials/tutorial.md](docs/tutorials/tutorial.md)** - Getting started tutorial

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation in `/docs`
- Review the development plan in the project docs

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
