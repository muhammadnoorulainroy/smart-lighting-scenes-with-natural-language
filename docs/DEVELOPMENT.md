# Development Guide

## Project Structure

### Backend (Spring Boot)

```
backend/
├── src/main/java/
│   └── com/example/smart/lighting/
│       ├── config/         # Configuration classes
│       ├── controller/     # REST controllers
│       ├── service/        # Business logic
│       ├── repository/     # Data access layer
│       ├── entity/         # JPA entities
│       ├── dto/            # Data transfer objects
│       ├── security/       # Security configuration
│       ├── websocket/      # WebSocket handlers
│       └── mqtt/           # MQTT integration
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/       # Flyway migrations
└── build.gradle.kts
```

### Frontend (Vue 3)

```
frontend/
├── src/
│   ├── assets/            # Static assets
│   ├── components/        # Reusable components
│   ├── views/             # Page components
│   ├── router/            # Vue Router config
│   ├── stores/            # Pinia stores
│   ├── api/               # API client
│   └── utils/             # Utility functions
├── public/                # Public assets
└── vite.config.js         # Vite configuration
```

### Mobile (Android/Kotlin)

```
mobile/
├── app/src/main/
│   ├── java/com/smartlighting/mobile/
│   │   ├── ui/           # Composables and screens
│   │   ├── data/         # Data layer
│   │   ├── domain/       # Business logic
│   │   ├── di/           # Dependency injection
│   │   └── util/         # Utilities
│   └── res/              # Resources
└── build.gradle.kts
```

## Setting Up Development Environment

### 1. Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized JavaScript origins: `http://localhost:5173`
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`
5. Copy Client ID and Client Secret to `.env`

### 2. Backend Development

#### Running locally:

```bash
cd backend
./gradlew bootRun
```

#### Hot reload:
- Install Spring Boot DevTools
- Changes to Java files will trigger automatic restart

#### Database migrations:
- Place SQL files in `src/main/resources/db/migration/`
- Follow naming: `V1__Description.sql`
- Migrations run automatically on startup

#### API Documentation:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

### 3. Frontend Development

#### Running locally:

```bash
cd frontend
npm run dev
```

#### Component Development:
- Use Vue DevTools browser extension
- Components hot-reload automatically
- Tailwind CSS classes are auto-completed with IDE plugins

#### State Management:
- Pinia stores in `src/stores/`
- Example store:

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

### 4. Mobile Development

#### Setup Android Studio:
1. Open `mobile/` directory in Android Studio
2. Sync Gradle files
3. Create an emulator or connect device
4. Run the app

#### Jetpack Compose Tips:
- Use `@Preview` annotations for component preview
- Enable Layout Inspector for debugging
- Use Compose compiler metrics for optimization

## API Endpoints

### Authentication
- `GET /oauth2/authorization/google` - Initiate Google OAuth
- `GET /api/me` - Get current user
- `POST /api/auth/logout` - Logout

### Rooms & Devices
- `GET /api/rooms` - List all rooms
- `POST /api/rooms` - Create room (ADMIN)
- `GET /api/devices` - List all devices
- `POST /api/devices` - Create device (ADMIN)

### Scenes & Control
- `GET /api/scenes` - List scenes
- `POST /api/scenes` - Create scene
- `POST /api/act/scene/{id}` - Apply scene
- `POST /api/act/device/{id}` - Control device

### Rules & Schedules
- `GET /api/rules` - List rules
- `POST /api/rules` - Create rule
- `GET /api/schedules` - List schedules

## WebSocket Events

Connect to `/ws` endpoint for real-time updates:

```javascript
const socket = new WebSocket('ws://localhost:8080/ws');

socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  // Handle different event types
  switch(data.type) {
    case 'DEVICE_STATE_CHANGE':
      // Update UI
      break;
    case 'SCENE_APPLIED':
      // Show notification
      break;
  }
};
```

## MQTT Integration

### Publishing Commands:

```java
@Service
public class MqttService {
    public void sendCommand(String room, String device, CommandDto command) {
        String topic = String.format("home/%s/%s/cmd", room, device);
        mqttClient.publish(topic, command.toJson(), 1, false);
    }
}
```

### Subscribing to State:

```java
mqttClient.subscribe("home/+/+/state", (topic, message) -> {
    // Parse topic and message
    // Update database
    // Emit WebSocket event
});
```

## Testing

### Backend Testing:

```java
@SpringBootTest
@AutoConfigureMockMvc
class LightingControllerTest {
    @Test
    void testApplyScene() {
        // Test implementation
    }
}
```

### Frontend Testing:

```javascript
import { mount } from '@vue/test-utils'
import RoomTile from '@/components/RoomTile.vue'

test('room tile renders', () => {
  const wrapper = mount(RoomTile, {
    props: { room: { name: 'Living Room' } }
  })
  expect(wrapper.text()).toContain('Living Room')
})
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
- Enable debug logging: `logging.level.com.example=DEBUG`
- Use Spring Boot Actuator endpoints
- Monitor SQL queries: `spring.jpa.show-sql=true`

### Frontend:
- Vue DevTools for component inspection
- Network tab for API calls
- Console logging with structured data

### Mobile:
- Logcat for Android logs
- Network profiler for API calls
- Layout Inspector for UI debugging

## Performance Optimization

### Backend:
- Use database indexes appropriately
- Implement caching with Redis
- Optimize N+1 queries with JPA fetch joins

### Frontend:
- Lazy load routes
- Use virtual scrolling for long lists
- Optimize bundle size with tree-shaking

### Mobile:
- Use remember in Compose for expensive operations
- Implement proper pagination
- Cache API responses locally

## Deployment Checklist

- [ ] Update environment variables
- [ ] Run database migrations
- [ ] Build production assets
- [ ] Configure SSL certificates
- [ ] Set up monitoring
- [ ] Configure backup strategy
- [ ] Test OAuth redirect URLs
- [ ] Verify MQTT broker settings
