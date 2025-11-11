# Smart Lighting Scenes - Build System
# Orchestrates builds across backend (Gradle) and frontend (npm)

.PHONY: help install build test clean run dev docker-up docker-down lint format package check-deps verify all

# Detect OS
ifeq ($(OS),Windows_NT)
	GRADLEW := gradlew.bat
	SHELL := cmd
	RM := del /Q /F
	RMDIR := rmdir /S /Q
	MKDIR := mkdir
	SEP := \\
else
	GRADLEW := ./gradlew
	SHELL := /bin/bash
	.SHELLFLAGS := -eu -o pipefail -c
	RM := rm -f
	RMDIR := rm -rf
	MKDIR := mkdir -p
	SEP := /
endif

# Directories
BACKEND_DIR := backend
FRONTEND_DIR := frontend
INFRA_DIR := infra

# Colors for output (disable on Windows)
ifeq ($(OS),Windows_NT)
	BLUE :=
	GREEN :=
	YELLOW :=
	RED :=
	NC :=
else
	BLUE := \033[0;34m
	GREEN := \033[0;32m
	YELLOW := \033[0;33m
	RED := \033[0;31m
	NC := \033[0m
endif

help:
	@echo "$(BLUE)Smart Lighting Scenes - Build System$(NC)"
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
	@echo "Infrastructure:"
	@echo "  make docker-up        Start infrastructure services"
	@echo "  make docker-down      Stop infrastructure"
	@echo "  make docker-logs      View logs"
	@echo "  make docker-ps        Show running containers"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean            Remove build artifacts"
	@echo "  make clean-all        Deep clean (includes node_modules)"
	@echo ""

# Prerequisites check
check-deps:
	@echo "$(BLUE)Checking prerequisites...$(NC)"
	@java --version >/dev/null 2>&1 || (echo "$(RED)Error: Java not found. Please install JDK 21+$(NC)" && exit 1)
	@node --version >/dev/null 2>&1 || (echo "$(RED)Error: Node.js not found. Please install Node.js 18+$(NC)" && exit 1)
	@npm --version >/dev/null 2>&1 || (echo "$(RED)Error: npm not found$(NC)" && exit 1)
	@docker --version >/dev/null 2>&1 || echo "$(YELLOW)Warning: Docker not found (optional)$(NC)"
	@test -f $(BACKEND_DIR)/gradlew || (echo "$(RED)Error: Gradle wrapper not found$(NC)" && exit 1)
	@echo "$(GREEN)All prerequisites found$(NC)"

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
	@echo "$(GREEN)Build complete$(NC)"

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
	@echo "$(GREEN)All tests passed$(NC)"

test-backend:
	@echo "Running backend tests..."
	cd $(BACKEND_DIR) && $(GRADLEW) test
	@echo "Test report: $(BACKEND_DIR)/build/reports/tests/test/index.html"

test-frontend:
	@echo "$(BLUE)Running frontend tests...$(NC)"
	@if [ -f "$(FRONTEND_DIR)/package.json" ] && grep -q '"test"' "$(FRONTEND_DIR)/package.json"; then \
		cd $(FRONTEND_DIR) && npm run test; \
	else \
		echo "$(YELLOW)Frontend tests not configured yet$(NC)"; \
	fi

# Code quality
lint: lint-backend lint-frontend
	@echo "$(GREEN)Linting complete$(NC)"

lint-backend:
	@echo "Linting backend..."
ifeq ($(OS),Windows_NT)
	@echo "Checkstyle not configured"
else
	@if cd $(BACKEND_DIR) && $(GRADLEW) tasks --all | grep -q checkstyle; then \
		cd $(BACKEND_DIR) && $(GRADLEW) checkstyleMain checkstyleTest; \
	else \
		echo "Checkstyle not configured"; \
	fi
endif

lint-frontend:
	@echo "Linting frontend..."
	cd $(FRONTEND_DIR) && npm run lint

format:
	@echo "$(BLUE)Formatting code...$(NC)"
	@if [ -f "$(FRONTEND_DIR)/package.json" ] && grep -q '"format"' "$(FRONTEND_DIR)/package.json"; then \
		cd $(FRONTEND_DIR) && npm run format; \
	fi
	@echo "$(GREEN)Code formatted$(NC)"

# Development
dev:
	@echo "$(BLUE)Development Mode$(NC)"
	@echo ""
	@echo "Start the application:"
	@echo "  1. make docker-up      (start infrastructure)"
	@echo "  2. make dev-backend    (terminal 1)"
	@echo "  3. make dev-frontend   (terminal 2)"
	@echo ""
	@echo "Access points:"
	@echo "  Frontend:  http://localhost:5173"
	@echo "  Backend:   http://localhost:8080"
	@echo "  API Docs:  http://localhost:8080/swagger-ui.html"
	@echo ""

dev-backend:
	@echo "Starting backend (http://localhost:8080)..."
	cd $(BACKEND_DIR) && $(GRADLEW) bootRun

dev-frontend:
	@echo "Starting frontend (http://localhost:5173)..."
	cd $(FRONTEND_DIR) && npm run dev

# Docker infrastructure
docker-up:
	@echo "$(BLUE)Starting infrastructure...$(NC)"
	@docker-compose -f $(INFRA_DIR)/docker-compose.yml up -d
	@echo "$(GREEN)Infrastructure started$(NC)"
	@echo ""
	@echo "Services:"
	@echo "  PostgreSQL:  localhost:5432"
	@echo "  Redis:       localhost:6379"
	@echo "  MQTT:        localhost:1883"
	@echo "  Adminer:     http://localhost:8090"
	@echo ""

docker-down:
	@echo "$(BLUE)Stopping infrastructure...$(NC)"
	@docker-compose -f $(INFRA_DIR)/docker-compose.yml down
	@echo "$(GREEN)Infrastructure stopped$(NC)"

docker-logs:
	@docker-compose -f $(INFRA_DIR)/docker-compose.yml logs -f

docker-ps:
	@docker-compose -f $(INFRA_DIR)/docker-compose.yml ps

# Packaging
package: build
	@echo "$(BLUE)Creating deployment packages...$(NC)"
	@echo "Backend JAR: $(BACKEND_DIR)/build/libs/"
	@ls -lh $(BACKEND_DIR)/build/libs/*.jar 2>/dev/null || true
	@echo "Frontend dist: $(FRONTEND_DIR)/dist/"
	@if [ -d "$(FRONTEND_DIR)/dist" ]; then du -sh $(FRONTEND_DIR)/dist; fi
	@echo "$(GREEN)Packages ready$(NC)"

# Cleanup
clean: clean-backend clean-frontend
	@echo "$(GREEN)Clean complete$(NC)"

clean-backend:
	@echo "Cleaning backend..."
	cd $(BACKEND_DIR) && $(GRADLEW) clean

clean-frontend:
	@echo "$(BLUE)Cleaning frontend...$(NC)"
	@if [ -d "$(FRONTEND_DIR)/dist" ]; then rm -rf $(FRONTEND_DIR)/dist; fi
	@if [ -d "$(FRONTEND_DIR)/.vite" ]; then rm -rf $(FRONTEND_DIR)/.vite; fi

clean-all: clean
	@echo "$(BLUE)Deep cleaning...$(NC)"
	@rm -rf node_modules $(FRONTEND_DIR)/node_modules
	@rm -f package-lock.json $(FRONTEND_DIR)/package-lock.json
	@echo "$(GREEN)Deep clean complete$(NC)"

# Verification
verify: build test lint
	@echo "$(GREEN)Verification passed$(NC)"

all: clean build test
	@echo "$(GREEN)Build pipeline complete$(NC)"

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
