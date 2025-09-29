# Smart Lighting Scenes with Natural Language

A comprehensive smart home lighting control system that understands natural language commands and creates intelligent lighting scenes.

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
├── embedded/         # ESP32 MicroPython code (future)
├── infra/            # Docker Compose and infrastructure
├── docs/             # Additional documentation
└── shared/           # Shared types and utilities
```

## Quick Start

### Prerequisites

- Node.js 18+ and npm
- Java 17+ (for Spring Boot)
- Docker and Docker Compose
- Android Studio (for mobile development)
- Google Cloud Console account (for OAuth)

### 1. Clone and Install

```bash
# Clone the repository
git clone https://github.com/yourusername/smart-lighting-scenes.git
cd smart-lighting-scenes

# Install root dependencies
npm install

# Install frontend dependencies
cd frontend && npm install
cd ..
```

### 2. Configure Environment

```bash
# Copy the example environment file
cp infra/env.example .env

# Edit .env with your configuration:
# - Google OAuth credentials
# - Database passwords
# - JWT secret
```

### 3. Start Infrastructure

```bash
# Start all infrastructure services
docker-compose -f infra/docker-compose.yml up -d

# Verify services are running
docker-compose -f infra/docker-compose.yml ps
```

Services will be available at:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Mosquitto MQTT: `localhost:1883` (WebSocket: `localhost:9001`)
- Mailhog: `http://localhost:8025`
- Adminer: `http://localhost:8090`

### 4. Start Backend

```bash
cd backend
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

### 5. Start Frontend

```bash
cd frontend
npm run dev
```

The web app will be available at `http://localhost:5173`

### 6. Build Mobile App

```bash
cd mobile
./gradlew assembleDebug
```

The APK will be generated in `mobile/app/build/outputs/apk/debug/`

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

## 📡 MQTT Topics

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

## Development

### Running Tests

```bash
# Backend tests
cd backend && ./gradlew test

# Frontend tests
cd frontend && npm run test

# Mobile tests
cd mobile && ./gradlew test
```

### Code Style

- Backend: Follows Spring Boot conventions
- Frontend: ESLint + Prettier
- Mobile: Kotlin code style

### Available Scripts

```bash
# Development
npm run dev:frontend    # Start Vue dev server
npm run dev:backend     # Start Spring Boot

# Building
npm run build:frontend  # Build Vue app
npm run build:backend   # Build Spring Boot JAR

# Docker
npm run docker:up       # Start infrastructure
npm run docker:down     # Stop infrastructure
npm run docker:logs     # View logs
```

## Mobile App Features

- **Google Sign-In**: Seamless authentication
- **Real-time Control**: Instant lighting adjustments
- **Voice Commands**: Natural language input via microphone
- **Scene Management**: Quick access to preset scenes
- **Push Notifications**: Updates on schedule triggers

## Future Enhancements

- [ ] ESP32 hardware integration
- [ ] Local LLaMA model integration
- [ ] Advanced scheduling with ML predictions
- [ ] Energy usage analytics
- [ ] Multi-home support
- [ ] Apple HomeKit integration

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## Acknowledgments

- Spring Boot team for the excellent framework
- Vue.js team for the reactive framework
- Google for OAuth and Material Design
- Eclipse for Mosquitto MQTT broker

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation in `/docs`
- Review the development plan in the project docs
