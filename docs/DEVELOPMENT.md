# Development Guide

## Project Structure

### Backend (Spring Boot)

```
backend/
├── src/main/java/
│   └── com/example/smart/lighting/scenes/with_natural/language/
│       ├── config/              # Configuration classes
│       │   ├── AppConfig.java
│       │   ├── DotenvConfig.java
│       │   ├── MqttConfig.java
│       │   ├── SecurityConfig.java
│       │   ├── StartupRunner.java
│       │   └── WebSocketConfig.java
│       ├── controller/          # REST controllers
│       │   ├── AuthController.java
│       │   ├── AutomationController.java
│       │   ├── ConfigController.java      # System settings
│       │   ├── DevicesController.java
│       │   ├── EventsController.java
│       │   ├── LightingController.java
│       │   ├── NlpController.java         # Natural language
│       │   ├── RoomsController.java
│       │   ├── ScenesController.java
│       │   ├── SchedulesController.java
│       │   └── UsersController.java
│       ├── service/             # Business logic
│       │   ├── AutomationService.java
│       │   ├── ConfigService.java         # Runtime config
│       │   ├── MqttService.java
│       │   ├── NlpService.java            # OpenAI integration
│       │   ├── OpenAIService.java
│       │   ├── ScheduleConflictService.java # Conflict detection
│       │   └── SchedulerService.java
│       ├── repository/          # Data access layer
│       │   ├── DeviceRepository.java
│       │   ├── NlpCommandRepository.java
│       │   ├── RoomRepository.java
│       │   ├── SceneRepository.java
│       │   ├── ScheduleRepository.java
│       │   ├── SystemConfigRepository.java
│       │   └── UserRepository.java
│       ├── entity/              # JPA entities
│       │   ├── Device.java
│       │   ├── NlpCommand.java
│       │   ├── Room.java
│       │   ├── Scene.java
│       │   ├── Schedule.java
│       │   ├── SystemConfig.java
│       │   └── User.java
│       ├── dto/                 # Data transfer objects
│       │   ├── ConflictAnalysisDto.java
│       │   ├── DeviceDto.java
│       │   ├── NlpCommandDto.java
│       │   ├── RoomDto.java
│       │   ├── SceneDto.java
│       │   ├── ScheduleDto.java
│       │   └── UserDto.java
│       ├── security/            # Security configuration
│       │   ├── CustomOAuth2User.java
│       │   ├── CustomOAuth2UserService.java
│       │   ├── OAuth2AuthenticationFailureHandler.java
│       │   └── OAuth2AuthenticationSuccessHandler.java
│       └── websocket/           # WebSocket handlers
│           ├── WebSocketEventService.java
│           └── WebSocketMessage.java
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/            # Flyway migrations
│       ├── V1__Initial_schema.sql
│       ├── V10__Add_scenes_and_schedules.sql
│       └── V11__Add_system_config.sql
└── build.gradle.kts
```

### Frontend (Vue 3)

```
frontend/
├── src/
│   ├── api/                     # API client modules
│   │   ├── auth.js
│   │   ├── axios.js             # Base API client
│   │   ├── config.js            # System settings API
│   │   ├── devices.js
│   │   ├── events.js
│   │   ├── lighting.js          # Mode control
│   │   ├── nlp.js               # Natural language API
│   │   ├── rooms.js
│   │   ├── scenes.js
│   │   ├── schedules.js
│   │   └── users.js
│   ├── components/              # Reusable components
│   │   ├── rooms/
│   │   ├── schedules/
│   │   ├── ThemeToggle.vue
│   │   └── UserMenu.vue
│   ├── views/                   # Page components
│   │   ├── AuthCallbackView.vue
│   │   ├── DashboardView.vue
│   │   ├── HomeView.vue
│   │   ├── NotFoundView.vue
│   │   ├── RoomsView.vue
│   │   ├── RoutinesView.vue
│   │   ├── ScenesView.vue       # With voice input
│   │   ├── SchedulesView.vue    # With conflict resolution
│   │   └── SettingsView.vue     # System configuration
│   ├── router/                  # Vue Router config
│   │   └── index.js
│   ├── stores/                  # Pinia stores
│   │   └── auth.js
│   └── utils/                   # Utility functions
│       ├── logger.js
│       └── routeGuards.js
├── public/                      # Public assets
└── vite.config.js               # Vite configuration
```

### Embedded (ESP32 + nRF52840)

```
embedded/
├── esp32_controller_2/          # Main controller
│   ├── main.py                  # Entry point
│   ├── config.py                # Static defaults
│   ├── runtime_config.py        # Dynamic config
│   ├── boot.py                  # Boot sequence
│   ├── mqtt_client_async.py     # MQTT wrapper
│   ├── uart_receiver_async.py   # Sensor data receiver
│   ├── oled_display_async.py    # Display manager
│   ├── led_controller_async.py  # LED control
│   ├── sensor_logic_async.py    # Environmental effects
│   ├── wifi_provisioning.py     # Captive portal
│   ├── config_manager.py        # NVS storage
│   ├── config_bridge.py         # Config loader
│   ├── logger.py                # Logging utility
│   └── sh1107.py                # OLED driver
├── esp32_controller_1/          # BLE hub
│   ├── main.py
│   ├── ble_central.py
│   └── config.py
└── nrf52840_sensor_*/           # Sensors
    └── code.py                  # CircuitPython
```

### Mobile (Android/Kotlin)

```
mobile/
├── app/src/main/
│   ├── java/com/smartlighting/mobile/
│   │   ├── ui/                  # Composables and screens
│   │   ├── data/                # Data layer
│   │   ├── domain/              # Business logic
│   │   ├── di/                  # Dependency injection
│   │   └── util/                # Utilities
│   └── res/                     # Resources
└── build.gradle.kts
```

## Setting Up Development Environment

### 1. Prerequisites

```bash
# Check required tools
java --version    # 21+
node --version    # 18+
npm --version     # 9+
docker --version  # 20+
```

### 2. Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized JavaScript origins: `http://localhost:5173`, `http://localhost`
   - Authorized redirect URIs:
     - `http://localhost/login/oauth2/code/google` (Docker deployment)
     - `http://localhost:8080/login/oauth2/code/google` (local development)
5. Copy Client ID and Client Secret to `.env`

### 3. OpenAI API Setup (for NLP)

1. Go to [OpenAI Platform](https://platform.openai.com)
2. Create an API key
3. Add to `.env`: `OPENAI_API_KEY=sk-your-key`

### 4. Environment Configuration

Create a `.env` file in the project root:

```bash
# Google OAuth (required)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# JWT Secret
JWT_SECRET=your-jwt-secret

# OpenAI API Key (optional, for NLP features)
OPENAI_API_KEY=sk-your-key
```

### 5. Backend Development

#### Running locally:

```bash
cd backend
./gradlew bootRun
```

#### Hot reload:
- Spring Boot DevTools is included
- Changes to Java files trigger automatic restart
- Changes to resources trigger reload

#### Database migrations:
- Place SQL files in `src/main/resources/db/migration/`
- Follow naming: `V{version}__{Description}.sql`
- Migrations run automatically on startup
- Use `IF NOT EXISTS` for idempotent operations

#### Logging Configuration:

In `application.properties`:
```properties
logging.level.root=INFO
logging.level.com.example.smart.lighting=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

All services use `@Slf4j` annotation:
```java
@Slf4j
@Service
public class MyService {
    public void doSomething() {
        log.debug("Debug message");
        log.info("Info message");
        log.error("Error: {}", exception.getMessage());
    }
}
```

### 6. Frontend Development

#### Environment Configuration

Create `frontend/.env` for local development:

```bash
# Backend API URL (required for local development)
VITE_API_URL=http://localhost:8080
```

| Environment | `VITE_API_URL` Value | Result |
|-------------|---------------------|--------|
| **Docker/Production** | Empty or unset | Relative URLs → Nginx proxies to backend |
| **Local Development** | `http://localhost:8080` | Direct connection to backend |
| **Remote Backend** | `http://YOUR_IP:8080` | Direct connection to backend IP |

> **Note:** Vite requires environment variables to start with `VITE_` to be exposed to the browser.

#### Running locally:

```bash
cd frontend
npm install
npm run dev
```

#### Component Development:
- Use Vue DevTools browser extension
- Components hot-reload automatically
- Tailwind CSS classes auto-complete with IDE plugins

#### State Management:

Pinia stores in `src/stores/`:

```javascript
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: null
  }),
  actions: {
    async login(credentials) {
      // Implementation
    }
  }
})
```

#### API Clients:

Each API module in `src/api/`:

```javascript
// Example: src/api/nlp.js
import apiClient from './axios'

export const nlpApi = {
  async parse(text) {
    const response = await apiClient.post('/api/nlp/parse', { text })
    return response.data
  }
}
```

### 7. Mobile Development

#### Backend Connection Configuration

To connect the Android app to your backend, update the `BASE_URL` in:

```
mobile/app/src/main/java/com/smartlighting/mobile/util/Constants.kt
```

```kotlin
const val BASE_URL = "http://YOUR_IP:8080/"
```

Replace `YOUR_IP` with your computer's local IP address (e.g., `192.168.1.100`).

> **Note:** The mobile app uses cleartext HTTP. Ensure your IP is listed in `mobile/app/src/main/res/xml/network_security_config.xml` for Android to allow the connection.

#### Setup Android Studio:
1. Open `mobile/` directory in Android Studio
2. Sync Gradle files
3. Update `BASE_URL` in `Constants.kt` with your backend IP
4. Create an emulator or connect device
5. Run the app

#### Jetpack Compose Tips:
- Use `@Preview` annotations for component preview
- Enable Layout Inspector for debugging
- Use Compose compiler metrics for optimization

## API Endpoints

### Authentication
- `GET /oauth2/authorization/google` - Initiate Google OAuth
- `GET /api/me` - Get current user
- `POST /api/auth/logout` - Logout

### Natural Language Processing
- `POST /api/nlp/parse` - Parse command (preview)
- `POST /api/nlp/execute` - Execute command
- `POST /api/nlp/confirm` - Confirm parsed command
- `POST /api/nlp/resolve-conflict` - Resolve schedule conflict

### Rooms & Devices
- `GET /api/rooms` - List all rooms
- `POST /api/rooms` - Create room (OWNER)
- `GET /api/devices` - List all devices
- `POST /api/devices` - Create device (OWNER)

### Scenes & Control
- `GET /api/scenes` - List scenes
- `POST /api/scenes` - Create scene
- `POST /api/scenes/{id}/apply` - Apply scene
- `POST /api/lighting/mode` - Set auto/manual mode
- `POST /api/lighting/room/{name}` - Control room

### Schedules
- `GET /api/schedules` - List schedules
- `POST /api/schedules` - Create schedule
- `POST /api/schedules/{id}/toggle` - Enable/disable
- `GET /api/schedules/conflicts` - Get conflicts

### Configuration (OWNER)
- `GET /api/config` - Get all config
- `PUT /api/config` - Update all config
- `GET /api/config/{category}` - Get category
- `PUT /api/config/{category}` - Update category
- `POST /api/config/reset` - Reset to defaults
- `POST /api/config/sync` - Sync to devices

## WebSocket Events

Connect to `/ws` endpoint for real-time updates:

```javascript
// Docker deployment: ws://localhost/ws
// Local development: ws://localhost:8080/ws
const socket = new WebSocket('ws://localhost:8080/ws');

socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  switch(data.type) {
    case 'DEVICE_STATE_CHANGE':
      // Update device UI
      break;
    case 'SCENE_APPLIED':
      // Show notification
      break;
    case 'CONFIG_UPDATED':
      // Refresh settings
      break;
    case 'SCHEDULE_TRIGGERED':
      // Log event
      break;
  }
};
```

## MQTT Integration

### Publishing Commands:

```java
@Service
public class MqttService {
    public void sendCommand(String room, String device, Map<String, Object> command) {
        String topic = String.format("smartlighting/room/%s/power", room);
        mqttClient.publish(topic, toJson(command), 1, false);
    }
}
```

### Publishing Config:

```java
@Service
public class ConfigService {
    public void publishConfig(String category, Map<String, Object> config) {
        String topic = String.format("smartlighting/config/%s", category);
        mqttClient.publish(topic, toJson(config), 1, true);  // Retained
    }
}
```

### Subscribing to State:

```java
mqttClient.subscribe("smartlighting/status/#", (topic, message) -> {
    // Parse topic: smartlighting/status/led/0/state
    // Update database
    // Emit WebSocket event
});
```

## Testing

### Backend Testing:

```java
@SpringBootTest
@AutoConfigureMockMvc
class NlpControllerTest {
    @Test
    void testParseCommand() {
        mockMvc.perform(post("/api/nlp/parse")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"text\": \"turn on bedroom lights\"}")
            .with(oauth2Login()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.understood").value(true));
    }
}
```

```bash
cd backend
./gradlew test
# View report: build/reports/tests/test/index.html
```

### Frontend Testing:

```javascript
import { mount } from '@vue/test-utils'
import SettingsView from '@/views/SettingsView.vue'

test('settings page loads', async () => {
  const wrapper = mount(SettingsView)
  expect(wrapper.find('h1').text()).toContain('System Settings')
})
```

```bash
cd frontend
npm run test
npm run test:coverage
```

### Mobile Testing:

```kotlin
@Test
fun testLoginFlow() {
    composeTestRule.setContent {
        LoginScreen()
    }
    composeTestRule
        .onNodeWithText("Login with Google")
        .performClick()
}
```

## Debugging Tips

### Backend:
- Enable debug logging in `application.properties`
- Use Spring Boot Actuator: `http://localhost:8080/actuator/health`
- Monitor SQL: `spring.jpa.show-sql=true`
- Check MQTT: Subscribe to `smartlighting/#` to see all messages

### Frontend:
- Vue DevTools for component inspection
- Network tab for API calls
- Console logging with structured data
- Vite's HMR shows compilation errors

### Embedded:
- Serial console at 115200 baud
- Set `DEBUG = True` in config.py
- Watch MQTT topics: `mosquitto_sub -h localhost -t "smartlighting/#" -v`
- Memory check: Look for "Mem:" in heartbeat logs

### MQTT Debugging:
```bash
# Subscribe to all topics
mosquitto_sub -h localhost -t "smartlighting/#" -v

# Send test command
mosquitto_pub -h localhost -t "smartlighting/command/lights" -m "on"

# Send config update
mosquitto_pub -h localhost -t "smartlighting/config/lighting" -m '{"maxBrightness":80}'
```

## Performance Optimization

### Backend:
- Use database indexes appropriately
- Implement caching with Redis
- Optimize N+1 queries with JPA fetch joins
- Use async processing for MQTT publishes

### Frontend:
- Lazy load routes
- Use virtual scrolling for long lists
- Optimize bundle size with tree-shaking
- Debounce slider inputs on Settings page

### Embedded:
- Periodic `gc.collect()` calls
- Compact JSON for UART messages
- Cache runtime config overrides only
- Minimize retained MQTT messages

## Code Style

### Backend (Java)
- Follow Spring Boot conventions
- Use Lombok annotations (`@Slf4j`, `@Data`, etc.)
- Services handle business logic, controllers handle HTTP
- DTOs for API responses, Entities for database

### Frontend (Vue)
- Use Composition API with `<script setup>`
- ESLint + Prettier for formatting
- API modules in `src/api/`
- Pinia for state management

### Embedded (MicroPython)
- Use async/await for non-blocking I/O
- RuntimeConfig for dynamic settings
- Logger module for consistent output
- Compact JSON to save memory

## Deployment Checklist

- [ ] Update environment variables for production
- [ ] Run database migrations
- [ ] Build production frontend assets
- [ ] Configure SSL certificates
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy
- [ ] Update OAuth redirect URLs
- [ ] Verify MQTT broker settings
- [ ] Test NLP with production OpenAI key
- [ ] Verify ESP32 can reach production MQTT
