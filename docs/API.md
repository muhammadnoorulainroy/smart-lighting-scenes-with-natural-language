# API Specification

## Base URL
- Development: `http://localhost:8080/api`
- Production: `https://api.smartlighting.com/api`

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

#### List Users (ADMIN)
```http
GET /api/users
```

#### Update User Role (ADMIN)
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

#### Create Room (ADMIN)
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

#### Update Room (ADMIN)
```http
PUT /api/rooms/{roomId}
```

#### Delete Room (ADMIN)
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

#### Create Device (ADMIN)
```http
POST /api/devices
```

Body:
```json
{
  "roomId": "uuid",
  "type": "LIGHT",
  "name": "Desk Lamp",
  "mqttCmdTopic": "home/bedroom/desk_lamp/cmd",
  "mqttStateTopic": "home/bedroom/desk_lamp/state",
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

#### Create Scene
```http
POST /api/scenes
```

Body:
```json
{
  "name": "Movie Time",
  "description": "Dim lighting for movies",
  "actions": [
    {
      "deviceId": "uuid",
      "on": true,
      "brightness": 20,
      "colorTemp": "warm"
    }
  ],
  "global": false
}
```

#### Apply Scene
```http
POST /api/act/scene/{sceneId}
```

Response:
```json
{
  "success": true,
  "devicesAffected": 3,
  "eventId": "uuid"
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

### Natural Language & Rules

#### Parse Natural Language
```http
POST /api/nl/parse
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
  "dsl": {
    "triggers": [
      {
        "type": "CRON",
        "expression": "0 7 * * MON-FRI"
      }
    ],
    "conditions": [],
    "actions": [
      {
        "type": "SET_DEVICE_STATE",
        "target": "living_room_lights",
        "state": {
          "on": true,
          "brightness": 50,
          "colorTemp": "warm"
        }
      }
    ]
  },
  "explanation": "This will turn on your living room lights to 50% brightness with warm color every weekday at 7:00 AM"
}
```

#### Parse and Execute
```http
POST /api/nl/parse_and_execute
```

Body:
```json
{
  "text": "Dim the bedroom lights to 25% now"
}
```

#### List Rules
```http
GET /api/rules
```

#### Create Rule
```http
POST /api/rules
```

Body:
```json
{
  "name": "Morning Routine",
  "description": "Weekday morning lighting",
  "priority": 100,
  "jsonDsl": {
    "triggers": [...],
    "conditions": [...],
    "actions": [...]
  },
  "naturalLanguageInput": "Original text command",
  "enabled": true
}
```

#### Update Rule
```http
PUT /api/rules/{ruleId}
```

#### Delete Rule
```http
DELETE /api/rules/{ruleId}
```

#### Enable/Disable Rule
```http
POST /api/rules/{ruleId}/toggle
```

### Schedules

#### List Schedules
```http
GET /api/schedules
```

Query Parameters:
- `from` (datetime): Start date
- `to` (datetime): End date
- `active` (boolean): Only active schedules

Response:
```json
[
  {
    "id": "uuid",
    "ruleId": "uuid",
    "ruleName": "Evening Lights",
    "nextFireAt": "2025-01-01T18:00:00Z",
    "lastFiredAt": "2024-12-31T18:00:00Z",
    "active": true
  }
]
```

#### Get Schedule Conflicts
```http
GET /api/schedules/conflicts
```

Response:
```json
[
  {
    "device": "living_room_light",
    "conflicts": [
      {
        "rule1Id": "uuid",
        "rule1Name": "Evening Scene",
        "rule2Id": "uuid", 
        "rule2Name": "Movie Time",
        "overlapTime": "2025-01-01T20:00:00Z",
        "suggestion": "Consider using priority or merging rules"
      }
    ]
  }
]
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
        "devicesAffected": 2
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
  "explanation": "The 'Evening Lights' rule triggered at sunset (6:47 PM) because today is Monday and the condition 'weekday' was met. This caused the living room lights to turn on at 60% brightness with warm color temperature.",
  "chain": [
    {
      "step": 1,
      "type": "TRIGGER",
      "description": "Sunset trigger activated at 6:47 PM"
    },
    {
      "step": 2,
      "type": "CONDITION",
      "description": "Weekday condition evaluated to true"
    },
    {
      "step": 3,
      "type": "ACTION",
      "description": "Living room lights set to 60% warm"
    }
  ]
}
```

### System Settings (ADMIN)

#### Get Settings
```http
GET /api/settings
```

Response:
```json
{
  "mqtt": {
    "host": "localhost",
    "port": 1883
  },
  "location": {
    "latitude": 37.7749,
    "longitude": -122.4194
  },
  "llm": {
    "model": "llama2",
    "apiUrl": "http://localhost:11434"
  }
}
```

#### Update Settings
```http
PUT /api/settings
```

## WebSocket Events

### Connection
```javascript
ws://localhost:8080/ws
```

### Event Types

#### Device State Change
```json
{
  "type": "DEVICE_STATE_CHANGE",
  "deviceId": "uuid",
  "state": {
    "on": true,
    "brightness": 75,
    "colorTemp": 350
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
  "affectedDevices": ["uuid1", "uuid2"],
  "timestamp": "2025-01-01T12:00:00Z"
}
```

#### Rule Triggered
```json
{
  "type": "RULE_TRIGGERED",
  "ruleId": "uuid",
  "ruleName": "Morning Routine",
  "timestamp": "2025-01-01T07:00:00Z"
}
```

#### Conflict Detected
```json
{
  "type": "CONFLICT_DETECTED",
  "conflictingRules": ["uuid1", "uuid2"],
  "device": "living_room_light",
  "resolution": "HIGHER_PRIORITY_WINS",
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
  "message": "Insufficient permissions",
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
