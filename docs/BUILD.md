# Build Automation

## Overview

| Component | Tool | Configuration |
|-----------|------|---------------|
| **Orchestration** | GNU Make | `Makefile` |
| **Backend** | Gradle 8.x | `backend/build.gradle.kts` |
| **Frontend** | npm + Vite | `frontend/package.json`, `frontend/vite.config.js` |
| **Infrastructure** | Docker Compose | `docker-compose.yml` |

---

## Prerequisites

| Software | Version | Check Command |
|----------|---------|---------------|
| Java JDK | 21+ | `java --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |
| Docker | 20+ | `docker --version` |
| Docker Compose | 2+ | `docker compose version` |
| GNU Make | 3.8+ | `make --version` |

### Recommended Shell (Windows)

On Windows, run `make` commands from **Git Bash** or **WSL** for full compatibility:

| Environment | Compatibility | Notes |
|-------------|---------------|-------|
| **Git Bash** | Full | Recommended for Windows |
| **WSL** | Full | Linux environment on Windows |
| **PowerShell** | Limited | Basic build/clean works, some features fail |
| **cmd.exe** | Not supported | Use Git Bash instead |

Git Bash is included with [Git for Windows](https://git-scm.com/download/win).

---

## Quick Start

```bash
# Full build
make all

# Or step by step:
make install    # Install dependencies
make build      # Build all components
make test       # Run tests
make docker-up  # Start services
```

---

## Build Commands

### Make (Orchestration)

```bash
make install      # Install all dependencies
make build        # Build backend + frontend
make test         # Run all tests
make docker-up    # Start Docker services
make docker-down  # Stop Docker services
make clean        # Remove build artifacts
make check-deps   # Verify prerequisites
```

### Backend (Gradle)

```bash
cd backend
./gradlew build       # Compile + test + package
./gradlew bootRun     # Run application
./gradlew test        # Run tests only
./gradlew bootJar     # Create executable JAR
./gradlew clean       # Clean build artifacts
```

**Output:** `backend/build/libs/Smart-Lighting-Scenes-0.0.1-SNAPSHOT.jar`

### Frontend (npm + Vite)

```bash
cd frontend
npm install        # Install dependencies
npm run dev        # Dev server with hot reload
npm run build      # Production build
npm run lint       # Code linting
npm run format     # Code formatting
```

**Output:** `frontend/dist/`

---

## Docker Services

```bash
# Start all services
docker compose up -d

# Start specific service
docker compose up -d backend

# Rebuild and start
docker compose up -d --build

# View logs
docker compose logs -f backend

# Stop all
docker compose down
```

---

## Environment Variables

Create `.env` file in project root:

```bash
# Authentication
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# NLP
OPENAI_API_KEY=sk-your-api-key

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=smartlighting
DB_USER=postgres
DB_PASSWORD=your-password

# MQTT
MQTT_BROKER=localhost
MQTT_PORT=1883

# Timezone (optional)
TZ=Europe/Paris
```

---

## Troubleshooting

### Common Fixes

```bash
# Clean rebuild
make clean
make install
make build

# Clear npm cache
cd frontend
rm -rf node_modules package-lock.json
npm install

# Refresh Gradle dependencies
cd backend
./gradlew build --refresh-dependencies

# Rebuild Docker containers
docker compose down
docker compose up -d --build
```

### Port Conflicts

```bash
# Check what's using a port
netstat -ano | findstr :8080    # Windows
lsof -i :8080                   # macOS/Linux

# Kill process by PID
taskkill /PID <PID> /F          # Windows
kill -9 <PID>                   # macOS/Linux
```

### Memory Issues

```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx2048m"

# Increase Node.js memory
export NODE_OPTIONS="--max-old-space-size=4096"
```

---

## Build Output Summary

| Component | Build Time | Output Size |
|-----------|------------|-------------|
| Backend | ~60s | ~45MB JAR |
| Frontend | ~20s | ~180KB (gzipped) |
