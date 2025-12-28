# API Specification

## Base URL
- Docker Deployment: `http://localhost/api` (through Nginx)
- Local Development: `http://localhost:8080/api`

## Authentication

All endpoints except `/oauth2/*` require authentication via:
- Session cookie (web)
- Bearer token (mobile)

### Headers
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

## Endpoints

### Authentication & Users

#### Initiate Google OAuth
```http
GET /oauth2/authorization/google
```
Redirects to Google OAuth consent screen.

#### OAuth Callback
```http
GET /login/oauth2/code/google?code={code}&state={state}
```
Handled automatically by Spring Security.

#### Get Current User
```http
GET /api/me
```

Response:
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "pictureUrl": "https://...",
  "role": "RESIDENT",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

#### List Users (OWNER)
```http
GET /api/users
```

#### Update User Role (OWNER)
```http
PUT /api/users/{userId}/role
```

Body:
```json
{
  "role": "RESIDENT"
}
```

### Rooms

#### List Rooms
```http
GET /api/rooms
```

Response:
```json
[
  {
    "id": "uuid",
    "name": "Living Room",
    "description": "Main living area",
    "devices": [
      {
        "id": "uuid",
        "name": "Ceiling Light",
        "type": "LIGHT",
        "state": {
          "on": true,
          "brightness": 75,
          "colorTemp": 300
        }
      }
    ]
  }
]
```

#### Create Room (OWNER)
```http
POST /api/rooms
```

Body:
```json
{
  "name": "Kitchen",
  "description": "Cooking area"
}
```

#### Update Room (OWNER)
```http
PUT /api/rooms/{roomId}
```

#### Delete Room (OWNER)
```http
DELETE /api/rooms/{roomId}
```

### Devices

#### List Devices
```http
GET /api/devices
```

Query Parameters:
- `roomId` (optional): Filter by room

#### Create Device (OWNER)
```http
POST /api/devices
```

Body:
```json
{
  "roomId": "uuid",
  "type": "LIGHT",
  "name": "Desk Lamp",
  "mqttCmdTopic": "smartlighting/command/esp32-001/led/0",
  "mqttStateTopic": "smartlighting/status/esp32-001/led/0",
  "meta": {
    "manufacturer": "Philips",
    "model": "Hue Go"
  }
}
```

#### Get Device State
```http
GET /api/devices/{deviceId}/state
```

Response:
```json
{
  "deviceId": "uuid",
  "on": true,
  "brightnessPct": 50,
  "colorTempMired": 370,
  "rgbColor": "#FFE4B5",
  "lastSeen": "2025-01-01T12:00:00Z"
}
```

#### Control Device
```http
POST /api/act/device/{deviceId}
```

Body:
```json
{
  "on": true,
  "brightness": 80,
  "colorTemp": "warm"
}
```

### Scenes

#### List Scenes
```http
GET /api/scenes
```

Query Parameters:
- `global` (boolean): Include global scenes
- `ownerId` (uuid): Filter by owner

Response:
```json
[
  {
    "id": "uuid",
    "name": "Movie Time",
    "description": "Dim lighting for movies",
    "isPreset": false,
    "target": "living_room",
    "settings": {
      "brightness": 20,
      "colorTemp": "warm",
      "rgb": [255, 147, 41]
    },
    "createdAt": "2025-01-01T12:00:00Z"
  }
]
```

#### Create Scene
```http
POST /api/scenes
```

Body:
```json
{
  "name": "Movie Time",
  "description": "Dim lighting for movies",
  "target": "living_room",
  "settings": {
    "brightness": 20,
    "colorTemp": "warm",
    "rgb": [255, 147, 41]
  }
}
```

#### Apply Scene
```http
POST /api/scenes/{sceneId}/apply
```

Query Parameters:
- `target` (optional): Override target room (e.g., "bedroom", "all")

Response:
```json
{
  "success": true,
  "devicesAffected": 3,
  "target": "living_room"
}
```

#### Update Scene
```http
PUT /api/scenes/{sceneId}
```

#### Delete Scene
```http
DELETE /api/scenes/{sceneId}
```

### Natural Language Processing (NLP)

#### Parse Command (Preview)
```http
POST /api/nlp/parse
```

Body:
```json
{
  "text": "Turn on living room lights at 50% warm every weekday at 7am"
}
```

Response:
```json
{
  "understood": true,
  "commandType": "schedule",
  "intent": "create_schedule",
  "target": "living_room",
  "action": {
    "type": "light",
    "on": true,
    "brightness": 50,
    "colorTemp": "warm"
  },
  "schedule": {
    "cronExpression": "0 7 * * MON-FRI",
    "description": "Every weekday at 7:00 AM"
  },
  "explanation": "This will turn on your living room lights to 50% brightness with warm color every weekday at 7:00 AM",
  "conflictAnalysis": {
    "hasConflicts": false,
    "conflicts": []
  }
}
```

#### Execute Command
```http
POST /api/nlp/execute
```

Body:
```json
{
  "text": "Dim the bedroom lights to 25% now"
}
```

Response:
```json
{
  "success": true,
  "commandType": "immediate",
  "devicesAffected": 1,
  "message": "Bedroom lights dimmed to 25%"
}
```

#### Confirm Parsed Command
```http
POST /api/nlp/confirm
```

Body (from parse response):
```json
{
  "commandType": "schedule",
  "intent": "create_schedule",
  "target": "living_room",
  "action": {...},
  "schedule": {...}
}
```

#### Resolve Schedule Conflict
```http
POST /api/nlp/resolve-conflict
```

Body:
```json
{
  "scheduleId": "uuid",
  "resolutionId": "adjust_time",
  "params": {
    "newTime": "06:45"
  }
}
```

Response:
```json
{
  "success": true,
  "message": "Schedule adjusted to 6:45 AM to avoid conflict",
  "updatedSchedule": {...}
}
```

### Schedules

#### List Schedules
```http
GET /api/schedules
```

Query Parameters:
- `from` (datetime): Start date
- `to` (datetime): End date
- `enabled` (boolean): Only enabled schedules

Response:
```json
[
  {
    "id": "uuid",
    "name": "Morning Lights",
    "description": "Turn on lights every morning",
    "cronExpression": "0 7 * * *",
    "action": {
      "type": "scene",
      "sceneName": "morning",
      "target": "all"
    },
    "enabled": true,
    "nextFireAt": "2025-01-02T07:00:00Z",
    "lastFiredAt": "2025-01-01T07:00:00Z",
    "createdAt": "2025-01-01T00:00:00Z"
  }
]
```

#### Create Schedule
```http
POST /api/schedules
```

Body:
```json
{
  "name": "Evening Lights",
  "description": "Dim lights for evening",
  "cronExpression": "0 19 * * *",
  "action": {
    "type": "light",
    "on": true,
    "brightness": 60,
    "colorTemp": "warm"
  },
  "target": "living_room"
}
```

#### Update Schedule
```http
PUT /api/schedules/{scheduleId}
```

#### Delete Schedule
```http
DELETE /api/schedules/{scheduleId}
```

#### Toggle Schedule
```http
POST /api/schedules/{scheduleId}/toggle
```

#### Get Schedule Conflicts
```http
GET /api/schedules/conflicts
```

Response:
```json
{
  "hasConflicts": true,
  "conflicts": [
    {
      "schedule1": {
        "id": "uuid",
        "name": "Evening Scene"
      },
      "schedule2": {
        "id": "uuid",
        "name": "Movie Time"
      },
      "overlapTime": "2025-01-01T20:00:00Z",
      "affectedTarget": "living_room",
      "resolutions": [
        {
          "id": "adjust_time",
          "description": "Move 'Movie Time' to 20:30",
          "params": {"newTime": "20:30"}
        },
        {
          "id": "disable_one",
          "description": "Disable 'Evening Scene' on Fridays"
        },
        {
          "id": "merge",
          "description": "Merge both into a combined evening scene"
        }
      ]
    }
  ]
}
```

### System Configuration (OWNER)

#### Get All Config
```http
GET /api/config
```

Response:
```json
{
  "lighting": {
    "globalMode": "auto",
    "autoDimEnabled": true,
    "sensorOverrideEnabled": true,
    "minBrightness": 0,
    "maxBrightness": 100,
    "luxMin": 50,
    "luxMax": 2000
  },
  "climate": {
    "tempMin": 20,
    "tempMax": 28,
    "tempBlendStrength": 95,
    "humidityMin": 30,
    "humidityMax": 70,
    "saturationAtMinHumidity": 60,
    "saturationAtMaxHumidity": 100
  },
  "audio": {
    "discoEnabled": true,
    "audioThreshold": 25,
    "discoDuration": 3000,
    "discoSpeed": 100,
    "flashBrightness": 100
  },
  "display": {
    "oledAutoSleep": true,
    "oledTimeout": 15,
    "showSensorData": true,
    "showTime": true
  }
}
```

#### Get Config Category
```http
GET /api/config/{category}
```

Categories: `lighting`, `climate`, `audio`, `display`

#### Update Config Category
```http
PUT /api/config/{category}
```

Body:
```json
{
  "maxBrightness": 80,
  "autoDimEnabled": false
}
```

#### Update All Config
```http
PUT /api/config
```

Body:
```json
{
  "lighting": {...},
  "climate": {...},
  "audio": {...},
  "display": {...}
}
```

#### Reset Category to Defaults
```http
POST /api/config/{category}/reset
```

#### Reset All to Defaults
```http
POST /api/config/reset
```

#### Sync Config to Devices
```http
POST /api/config/sync
```

Manually pushes current config to all ESP32 devices via MQTT.

### Lighting Control

#### Set Mode (Auto/Manual)
```http
POST /api/lighting/mode
```

Body:
```json
{
  "mode": "auto",
  "target": "all"
}
```

#### Control Room Lights
```http
POST /api/lighting/room/{roomName}
```

Body:
```json
{
  "on": true,
  "brightness": 75,
  "rgb": [255, 200, 150]
}
```

### Events & Logs

#### List Events
```http
GET /api/events
```

Query Parameters:
- `type` (enum): Filter by event type
- `deviceId` (uuid): Filter by device
- `from` (datetime): Start time
- `to` (datetime): End time
- `page` (int): Page number
- `size` (int): Page size

Response:
```json
{
  "content": [
    {
      "id": "uuid",
      "timestamp": "2025-01-01T12:00:00Z",
      "type": "SCENE_APPLIED",
      "details": {
        "sceneName": "Reading",
        "devicesAffected": 2,
        "target": "bedroom"
      },
      "actorUserId": "uuid",
      "sceneId": "uuid"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

#### Explain Event
```http
GET /api/events/{eventId}/explain
```

Response:
```json
{
  "eventId": "uuid",
  "explanation": "The 'Evening Lights' schedule triggered at 7:00 PM. This caused the living room lights to turn on at 60% brightness with warm color temperature.",
  "chain": [
    {
      "step": 1,
      "type": "TRIGGER",
      "description": "Schedule triggered at 7:00 PM"
    },
    {
      "step": 2,
      "type": "ACTION",
      "description": "Living room lights set to 60% warm"
    }
  ]
}
```

## WebSocket Events

### Connection
```javascript
// Docker deployment
ws://localhost/ws

// Local development
ws://localhost:8080/ws
```

### Event Types

#### Device State Change
```json
{
  "type": "DEVICE_STATE_CHANGE",
  "deviceId": "uuid",
  "roomId": "uuid",
  "state": {
    "on": true,
    "brightness": 75,
    "rgb": [255, 200, 150]
  },
  "timestamp": "2025-01-01T12:00:00Z"
}
```

#### Scene Applied
```json
{
  "type": "SCENE_APPLIED",
  "sceneId": "uuid",
  "sceneName": "Movie Time",
  "target": "living_room",
  "affectedDevices": ["uuid1", "uuid2"],
  "timestamp": "2025-01-01T12:00:00Z"
}
```

#### Schedule Triggered
```json
{
  "type": "SCHEDULE_TRIGGERED",
  "scheduleId": "uuid",
  "scheduleName": "Morning Routine",
  "action": {...},
  "timestamp": "2025-01-01T07:00:00Z"
}
```

#### Config Updated
```json
{
  "type": "CONFIG_UPDATED",
  "category": "lighting",
  "changes": {
    "maxBrightness": 80
  },
  "updatedBy": "user@example.com",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

#### Conflict Detected
```json
{
  "type": "CONFLICT_DETECTED",
  "conflictingSchedules": ["uuid1", "uuid2"],
  "target": "living_room",
  "suggestedResolutions": [...],
  "timestamp": "2025-01-01T18:00:00Z"
}
```

## Error Responses

### 400 Bad Request
```json
{
  "error": "INVALID_REQUEST",
  "message": "Brightness must be between 0 and 100",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 401 Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Authentication required",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 403 Forbidden
```json
{
  "error": "FORBIDDEN",
  "message": "Insufficient permissions. OWNER role required.",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 404 Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Device not found",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 409 Conflict
```json
{
  "error": "SCHEDULE_CONFLICT",
  "message": "This schedule conflicts with 'Evening Lights'",
  "conflictDetails": {...},
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

## Rate Limiting

- Authenticated requests: 1000/hour
- Unauthenticated requests: 100/hour
- WebSocket messages: 100/minute

Headers in response:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1704139200
```
