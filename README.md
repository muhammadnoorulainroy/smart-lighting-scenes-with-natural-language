# Smart Lighting Scenes with Natural Language

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-brightgreen.svg)](https://vuejs.org/)

A comprehensive smart home lighting control system that understands natural language commands and creates intelligent lighting scenes. Control your home lighting through voice commands, scheduled automations, and real-time IoT integration.

## Features

- **Natural Language Control**: Simply speak or type commands like "dim the living room lights to 30% warm"
- **Smart Scheduling**: Create intelligent rules that adapt to sunrise/sunset and your daily routines
- **Multi-Platform Support**: Web dashboard, Android app, and ESP32 hardware integration
- **Real-time Updates**: WebSocket-powered live updates across all platforms
- **Conflict Resolution**: Smart conflict detection and resolution for overlapping rules
- **Role-Based Access**: Admin, Resident, and Guest roles with appropriate permissions

## Architecture

This is a monorepo containing all components of the Smart Lighting system:

```
smart-lighting-scenes/
├── backend/          # Spring Boot 3.x backend API
├── frontend/         # Vue 3 + Vite web application
├── mobile/           # Android app (Kotlin + Jetpack Compose)
├── embedded/         # ESP32 MicroPython code + WS2812B LEDs
│   ├── esp32-simulator/   # MicroPython application
│   ├── YOUR_HARDWARE_GUIDE.md   # Start here for hardware setup
│   ├── GETTING_STARTED.md       # Quick setup guide
│   └── test_mqtt.py             # MQTT testing tool
├── infra/            # Docker Compose and infrastructure
├── docs/             # Additional documentation
│   ├── SYSTEM_ARCHITECTURE.md      # Complete system design
│   └── EMBEDDED_DESIGN_SUMMARY.md  # Embedded system details
└── shared/           # Shared types and utilities
```

## Prerequisites

Before building, testing, or running the project, ensure you have:

- **Node.js 18+** and **npm 9+**
- **Java 21+** (JDK)
- **Docker** and **Docker Compose**
- **GNU Make** (optional, for simplified commands)
- Google Cloud Console account (for OAuth configuration)

**Verify prerequisites:**
```bash
# Check all required tools
make check-deps

# Or manually:
java --version    # Should be 21+
node --version    # Should be 18+
npm --version     # Should be 9+
docker --version  # Should be 20+
```

---

## How to Build

### Option 1: Build Everything (Recommended)

```bash
# Using Make (recommended)
make install    # Install all dependencies
make build      # Build backend + frontend

# Or manually:
npm install                          # Install root dependencies
cd backend && ./gradlew build        # Build backend (creates JAR)
cd ../frontend && npm install && npm run build  # Build frontend (creates dist/)
```

**Build outputs:**
- **Backend:** `backend/build/libs/Smart-Lighting-Scenes-0.0.1-SNAPSHOT.jar`
- **Frontend:** `frontend/dist/` (optimized production bundle)

### Option 2: Build Individual Components

**Backend only:**
```bash
cd backend
./gradlew build
# Output: backend/build/libs/*.jar
```

**Frontend only:**
```bash
cd frontend
npm install    # If not done already
npm run build
# Output: frontend/dist/
```

**Build time:** First build ~90 seconds, subsequent builds ~20 seconds (incremental)

---

## How to Test

### Run All Tests

```bash
# Using Make
make test

# Or manually:
cd backend && ./gradlew test          # Backend tests
cd ../frontend && npm run test        # Frontend tests
```

### Test Individual Components

**Backend tests (JUnit 5):**
```bash
cd backend
./gradlew test

# View test report:
# Open: backend/build/reports/tests/test/index.html
```

**Frontend tests:**
```bash
cd frontend
npm run test

# View coverage:
# Open: frontend/coverage/index.html
```

**Test output:**
- Backend: JUnit test reports in `backend/build/reports/`
- Frontend: Test results in console + coverage reports

---

## API Documentation

Generate reference documentation for both backend and frontend codebases.

### Backend (Javadoc)

```bash
cd backend
./gradlew docs

# Output: backend/build/docs/javadoc/index.html
```

The Javadoc includes documentation for:
- REST Controllers (`AuthController`, `DevicesController`)
- Services (`MqttService`)
- Entities (`Device`, `User`, `Room`, `DeviceState`)
- Repositories (`DeviceRepository`, `RoomRepository`)
- DTOs (`DeviceDto`, `UserDto`)
- Security configuration (`SecurityConfig`)

### Frontend (JSDoc)

```bash
cd frontend
npm install    # If not done already
npm run docs

# Output: frontend/docs/jsdoc/index.html
```

The JSDoc includes documentation for:
- API clients (`axios.js`, `auth.js`, `devices.js`, `rooms.js`)
- Pinia stores (`auth.js`)
- Route guards (`routeGuards.js`)
- Utilities (`logger.js`)

---

## How to Run

### Quick Start (Complete Setup)

```bash
# Step 1: Start infrastructure services (PostgreSQL, Redis, MQTT)
make docker-up
# Or: docker-compose -f infra/docker-compose.yml up -d

# Step 2: Start backend (in terminal 1)
make dev-backend
# Or: cd backend && ./gradlew bootRun

# Step 3: Start frontend (in terminal 2)
make dev-frontend
# Or: cd frontend && npm run dev
```

**Access the application:**
- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080
- **Database Admin:** http://localhost:8090 (Adminer)

### Run Production Build

**Backend (from JAR):**
```bash
# After building (make build)
java -jar backend/build/libs/Smart-Lighting-Scenes-0.0.1-SNAPSHOT.jar
```

**Frontend (serve dist):**
```bash
cd frontend
npm run preview
# Or deploy dist/ folder to any static host (Nginx, Apache, Netlify, etc.)
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

## Quick Start (First Time Setup)

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/smart-lighting-scenes.git
cd smart-lighting-scenes
```

### Step 2: Install Dependencies

```bash
make install
# This installs backend + frontend dependencies automatically
```

### Step 3: Configure Environment

```bash
# Copy the example environment file
cp infra/env.example .env

# Edit .env with your configuration:
# - Google OAuth credentials
# - Database passwords
# - JWT secret
```

### Step 4: Build Project

```bash
make build
# Builds both backend and frontend
```

### Step 5: Start Infrastructure

```bash
make docker-up
# Starts PostgreSQL, Redis, MQTT broker
```

### Step 6: Run Application

```bash
# In terminal 1 - Backend
make dev-backend

# In terminal 2 - Frontend  
make dev-frontend
```

**Access:** http://localhost:5173

---

## Detailed Documentation

For comprehensive build automation documentation, see:
- **[BUILD.md](BUILD.md)** - Complete build system documentation

**Key topics covered:**
- Tool selection rationale (Make + Gradle + npm)
- Dependency management (544+ packages)
- Build process deep-dive
- Testing automation
- Packaging procedures
- Troubleshooting guide

## UI Design

The system features a modern, sophisticated design with:
- **Color Scheme**: Warm yellows and greens (avoiding blue/purple)
- **Typography**: Clean, readable Inter font family
- **Responsive Design**: Works seamlessly on desktop and mobile
- **Dark Mode**: Full dark mode support across all platforms
- **Material Design 3**: Android app follows Material You guidelines

## Authentication

The system uses Google OAuth 2.0 for authentication:

1. Set up a Google Cloud Project
2. Enable Google+ API
3. Create OAuth 2.0 credentials
4. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google` (development)
   - Your production URLs

## MQTT Topics

The system uses the following MQTT topic structure:

```
home/<room>/<device>/cmd     # Commands (QoS 1, non-retained)
home/<room>/<device>/state   # State updates (QoS 1, retained)
home/<room>/<device>/tele    # Telemetry (QoS 0/1)
home/<room>/<device>/status  # Online/Offline (retained, LWT)
```

## Database Schema

The PostgreSQL database includes tables for:
- Users (with OAuth integration)
- Rooms and Devices
- Scenes and Rules
- Schedules and Events
- Device states and sensor readings

See `infra/init-db/01-schema.sql` for the complete schema.

---

## Development

### Code Style

- **Backend:** Spring Boot conventions, Lombok annotations
- **Frontend:** ESLint + Prettier for Vue 3
- **Build:** Gradle (backend), npm + Vite (frontend), Make (orchestration)

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

## Mobile App Features

- **Google Sign-In**: Seamless authentication
- **Real-time Control**: Instant lighting adjustments
- **Voice Commands**: Natural language input via microphone
- **Scene Management**: Quick access to preset scenes
- **Push Notifications**: Updates on schedule triggers

## Embedded System (ESP32 + WS2812B LEDs)

### Hardware Setup

The system supports real lighting control via ESP32 and WS2812B RGB LED breakout boards. Each LED represents a room and can be controlled independently.

**Quick Start:**
1. **Hardware Guide:** See [`embedded/YOUR_HARDWARE_GUIDE.md`](embedded/YOUR_HARDWARE_GUIDE.md) for wiring instructions
2. **Setup:** Follow [`embedded/GETTING_STARTED.md`](embedded/GETTING_STARTED.md) for complete setup (30 min)
3. **Testing:** Use `python embedded/test_mqtt.py --test-all` to test MQTT control

**Hardware Requirements:**
- ESP32 board (Adafruit HUZZAH32 recommended)
- WS2812B RGB LED breakout boards (one per room)
- Breadboard and jumper wires
- 330Ω resistor
- USB cable for power

**How It Works:**
```
User Command → Backend → MQTT Broker → ESP32 → WS2812B LEDs
                  ↓                        ↓
            WebSocket ←── State Update ────┘
```

Each LED is daisy-chained to a single GPIO pin (GPIO13) and mapped to a room:
- LED 0 = Bedroom
- LED 1 = Living Room
- LED 2 = Kitchen
- LED 3 = Bathroom

**Documentation:**
- [`embedded/README_OVERVIEW.md`](embedded/README_OVERVIEW.md) - Complete embedded documentation index
- [`docs/EMBEDDED_DESIGN_SUMMARY.md`](docs/EMBEDDED_DESIGN_SUMMARY.md) - System design and architecture
- [`docs/SYSTEM_ARCHITECTURE.md`](docs/SYSTEM_ARCHITECTURE.md) - Full system overview


## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request


## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation in `/docs`
- Review the development plan in the project docs

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
