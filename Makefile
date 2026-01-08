# Smart Lighting Scenes - Build System
# Orchestrates builds across backend (Gradle) and frontend (npm)
#
# Compatible with: Git Bash, WSL, Linux, macOS
# On Windows: Run from Git Bash (recommended) or WSL

.PHONY: help install build test clean run dev docker-up docker-down lint format package check-deps verify all

BASH_EXISTS := $(shell bash --version 2>/dev/null && echo yes)

ifdef BASH_EXISTS
    SHELL := bash
    .SHELLFLAGS := -eu -o pipefail -c
else
    $(warning WARNING: bash not found. Some features may not work. Install Git Bash on Windows.)
endif

# Detect OS for gradle wrapper
UNAME_S := $(shell uname -s 2>/dev/null || echo Windows)
IS_WINDOWS_BASH := $(filter MINGW% MSYS% CYGWIN%,$(UNAME_S))
IS_NATIVE_WINDOWS := $(filter Windows%,$(UNAME_S))

ifdef IS_WINDOWS_BASH
    GRADLEW := ./gradlew.bat
else ifdef IS_NATIVE_WINDOWS
    GRADLEW := gradlew.bat
else
    GRADLEW := ./gradlew
endif

RM := rm -f
RMDIR := rm -rf
MKDIR := mkdir -p

# Directories
BACKEND_DIR := backend
FRONTEND_DIR := frontend
INFRA_DIR := infra
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m

help:
	@printf "$(BLUE)Smart Lighting Scenes - Build System$(NC)\n"
	@echo ""
	@echo "Setup & Installation:"
	@echo "  make install          Install all dependencies"
	@echo "  make check-deps       Verify required tools"
	@echo ""
	@echo "Build & Package:"
	@echo "  make build            Build all projects"
	@echo "  make build-backend    Build backend only"
	@echo "  make build-frontend   Build frontend only"
	@echo "  make package          Create deployment packages"
	@echo ""
	@echo "Development:"
	@echo "  make dev              Start development mode (shows instructions)"
	@echo "  make dev-backend      Run backend dev server (port 8080)"
	@echo "  make dev-frontend     Run frontend dev server (port 5173)"
	@echo ""
	@echo "Testing & Quality:"
	@echo "  make test             Run all tests"
	@echo "  make test-backend     Run backend tests"
	@echo "  make test-frontend    Run frontend tests"
	@echo "  make lint             Run linters"
	@echo "  make format           Format code"
	@echo "  make verify           Full verification (build + test + lint)"
	@echo ""
	@echo "Docker - Full Stack:"
	@echo "  make docker-build     Build all Docker images"
	@echo "  make docker-up-app    Start full stack (frontend + backend + infra)"
	@echo "  make docker-down-app  Stop full stack"
	@echo "  make docker-logs-app  View application logs"
	@echo ""
	@echo "Docker - Infrastructure Only:"
	@echo "  make docker-up        Start infrastructure only (for local dev)"
	@echo "  make docker-down      Stop infrastructure"
	@echo "  make docker-logs      View infrastructure logs"
	@echo "  make docker-ps        Show running containers"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean            Remove build artifacts"
	@echo "  make clean-all        Deep clean (includes node_modules)"
	@echo ""

# Prerequisites check
check-deps:
	@printf "$(BLUE)Checking prerequisites...$(NC)\n"
	@java --version >/dev/null 2>&1 || (printf "$(RED)Error: Java not found. Please install JDK 21+$(NC)\n" && exit 1)
	@node --version >/dev/null 2>&1 || (printf "$(RED)Error: Node.js not found. Please install Node.js 18+$(NC)\n" && exit 1)
	@npm --version >/dev/null 2>&1 || (printf "$(RED)Error: npm not found$(NC)\n" && exit 1)
	@docker --version >/dev/null 2>&1 || printf "$(YELLOW)Warning: Docker not found (optional)$(NC)\n"
	@test -f $(BACKEND_DIR)/gradlew || test -f $(BACKEND_DIR)/gradlew.bat || (printf "$(RED)Error: Gradle wrapper not found$(NC)\n" && exit 1)
	@printf "$(GREEN)All prerequisites found$(NC)\n"

# Installation
install: check-deps
	@echo "Installing dependencies..."
	@echo "Installing root workspace..."
	npm install
	@echo "Installing frontend dependencies..."
	cd $(FRONTEND_DIR) && npm install
	@echo "Downloading backend dependencies..."
	cd $(BACKEND_DIR) && $(GRADLEW) build -x test --quiet
	@echo "Installation complete"

# Build
build: build-backend build-frontend
	@printf "$(GREEN)Build complete$(NC)\n"

build-backend:
	@echo "Building backend..."
	cd $(BACKEND_DIR) && $(GRADLEW) clean build -x test
	@echo "Backend built: $(BACKEND_DIR)/build/libs/*.jar"

build-frontend:
	@echo "Building frontend..."
	cd $(FRONTEND_DIR) && npm run build
	@echo "Frontend built: $(FRONTEND_DIR)/dist/"

# Testing
test: test-backend test-frontend
	@printf "$(GREEN)All tests passed$(NC)\n"

test-backend:
	@echo "Running backend tests..."
	cd $(BACKEND_DIR) && $(GRADLEW) test
	@echo "Test report: $(BACKEND_DIR)/build/reports/tests/test/index.html"

test-frontend:
	@printf "$(BLUE)Running frontend tests...$(NC)\n"
	@if [ -f "$(FRONTEND_DIR)/package.json" ] && grep -q '"test"' "$(FRONTEND_DIR)/package.json"; then \
		cd $(FRONTEND_DIR) && npm run test; \
	else \
		printf "$(YELLOW)Frontend tests not configured yet$(NC)\n"; \
	fi

# Code quality
lint: lint-backend lint-frontend
	@printf "$(GREEN)Linting complete$(NC)\n"

lint-backend:
	@echo "Linting backend..."
	@if cd $(BACKEND_DIR) && $(GRADLEW) tasks --all 2>/dev/null | grep -q checkstyle; then \
		cd $(BACKEND_DIR) && $(GRADLEW) checkstyleMain checkstyleTest; \
	else \
		echo "Checkstyle not configured"; \
	fi

lint-frontend:
	@echo "Linting frontend..."
	cd $(FRONTEND_DIR) && npm run lint

format:
	@printf "$(BLUE)Formatting code...$(NC)\n"
	@if [ -f "$(FRONTEND_DIR)/package.json" ] && grep -q '"format"' "$(FRONTEND_DIR)/package.json"; then \
		cd $(FRONTEND_DIR) && npm run format; \
	fi
	@printf "$(GREEN)Code formatted$(NC)\n"

# Development
dev:
	@printf "$(BLUE)Development Mode$(NC)\n"
	@echo ""
	@echo "Start the application:"
	@echo "  1. make docker-up      (start infrastructure)"
	@echo "  2. make dev-backend    (terminal 1)"
	@echo "  3. make dev-frontend   (terminal 2)"
	@echo ""
	@echo "Access points:"
	@echo "  Frontend:  http://localhost:5173"
	@echo "  Backend:   http://localhost:8080/api"
	@echo ""

dev-backend:
	@echo "Starting backend (http://localhost:8080)..."
	cd $(BACKEND_DIR) && $(GRADLEW) bootRun

dev-frontend:
	@echo "Starting frontend (http://localhost:5173)..."
	cd $(FRONTEND_DIR) && npm run dev

# Docker - Full Stack Deployment
docker-build:
	@echo "Building Docker images..."
	docker-compose build

docker-up-app:
	@echo "Starting full application stack..."
	docker-compose up -d
	@echo ""
	@echo "Application started:"
	@echo "  Frontend:    http://localhost"
	@echo "  Backend API: http://localhost:8080"
	@echo "  Database:    localhost:5432"
	@echo "  Redis:       localhost:6379"
	@echo "  MQTT:        localhost:1883"
	@echo ""

docker-up-app-dev:
	@echo "Starting full stack with dev tools..."
	docker-compose --profile dev up -d
	@echo ""
	@echo "Application started:"
	@echo "  Frontend:    http://localhost"
	@echo "  Backend API: http://localhost:8080"
	@echo "  Adminer:     http://localhost:8090"
	@echo ""

docker-down-app:
	@echo "Stopping application stack..."
	docker-compose down

docker-logs-app:
	docker-compose logs -f

docker-ps-app:
	docker-compose ps

# Docker - Infrastructure Only (for local development)
docker-up:
	@echo "Starting infrastructure only..."
	docker-compose -f $(INFRA_DIR)/docker-compose.yml up -d
	@echo ""
	@echo "Infrastructure started:"
	@echo "  PostgreSQL:  localhost:5432"
	@echo "  Redis:       localhost:6379"
	@echo "  MQTT:        localhost:1883"
	@echo "  Adminer:     http://localhost:8090"
	@echo ""

docker-down:
	@echo "Stopping infrastructure..."
	docker-compose -f $(INFRA_DIR)/docker-compose.yml down

docker-logs:
	docker-compose -f $(INFRA_DIR)/docker-compose.yml logs -f

docker-ps:
	docker-compose -f $(INFRA_DIR)/docker-compose.yml ps

# Packaging
package: build
	@printf "$(BLUE)Creating deployment packages...$(NC)\n"
	@echo "Backend JAR: $(BACKEND_DIR)/build/libs/"
	@ls -lh $(BACKEND_DIR)/build/libs/*.jar 2>/dev/null || true
	@echo "Frontend dist: $(FRONTEND_DIR)/dist/"
	@if [ -d "$(FRONTEND_DIR)/dist" ]; then du -sh $(FRONTEND_DIR)/dist; fi
	@printf "$(GREEN)Packages ready$(NC)\n"

# Cleanup
clean: clean-backend clean-frontend
	@printf "$(GREEN)Clean complete$(NC)\n"

clean-backend:
	@echo "Cleaning backend..."
	cd $(BACKEND_DIR) && $(GRADLEW) clean

clean-frontend:
	@printf "$(BLUE)Cleaning frontend...$(NC)\n"
	@$(RMDIR) $(FRONTEND_DIR)/dist 2>/dev/null || true
	@$(RMDIR) $(FRONTEND_DIR)/.vite 2>/dev/null || true

clean-all: clean
	@printf "$(BLUE)Deep cleaning...$(NC)\n"
	@$(RMDIR) node_modules $(FRONTEND_DIR)/node_modules 2>/dev/null || true
	@$(RM) package-lock.json $(FRONTEND_DIR)/package-lock.json 2>/dev/null || true
	@printf "$(GREEN)Deep clean complete$(NC)\n"

# Verification
verify: build test lint
	@printf "$(GREEN)Verification passed$(NC)\n"

all: clean build test
	@printf "$(GREEN)Build pipeline complete$(NC)\n"

# Project info
info:
	@echo "Project: Smart Lighting Scenes with Natural Language"
	@echo ""
	@echo "Components:"
	@echo "  Backend:   Spring Boot 3.x (Java 21)"
	@echo "  Frontend:  Vue 3 + Vite"
	@echo "  Database:  PostgreSQL 16"
	@echo "  Cache:     Redis 7"
	@echo "  MQTT:      Eclipse Mosquitto"
	@echo ""
	@echo "Build Tools:"
	@echo "  Make:      Orchestration"
	@echo "  Gradle:    Backend (8.x)"
	@echo "  npm/Vite:  Frontend"
	@echo ""
