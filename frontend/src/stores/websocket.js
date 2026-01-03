import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

// Reactive state for sensor data
const sensorData = reactive({})
const deviceStates = reactive({})
const sceneEvents = ref([])
const lastSceneApplied = ref(null)
const pendingScenes = reactive({})
const connected = ref(false)
const connectionError = ref(null)

let stompClient = null

/**
 * Initialize WebSocket connection to backend
 */
export function connectWebSocket() {
  if (stompClient && connected.value) {
    console.log('[WS] Already connected')
    return
  }

  const wsUrl = `${import.meta.env.VITE_API_URL ?? ''}/ws`
  console.log('[WS] Connecting to:', wsUrl)

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: str => {
      if (import.meta.env.DEV) {
        console.log('[STOMP]', str)
      }
    },
    onConnect: () => {
      console.log('[WS] Connected!')
      connected.value = true
      connectionError.value = null
      subscribeToTopics()
    },
    onDisconnect: () => {
      console.log('[WS] Disconnected')
      connected.value = false
    },
    onStompError: frame => {
      console.error('[WS] STOMP error:', frame)
      connectionError.value = frame.headers?.message || 'Connection error'
    },
    onWebSocketError: event => {
      console.error('[WS] WebSocket error:', event)
      connectionError.value = 'WebSocket connection failed'
    }
  })

  stompClient.activate()
}

/**
 * Subscribe to all relevant topics
 */
function subscribeToTopics() {
  if (!stompClient || !stompClient.connected) {
    return
  }

  // Subscribe to sensor updates
  stompClient.subscribe('/topic/sensors', message => {
    try {
      const data = JSON.parse(message.body)
      console.log('[WS] Sensor update:', data)
      // Handle sensor data: {type: "SENSOR_UPDATE", data: {sensorName, readings}}
      const sensorName = data.data?.sensorName
      const readings = data.data?.readings || {}
      if (sensorName) {
        sensorData[sensorName] = {
          ...readings,
          timestamp: data.timestamp
        }
        console.log('[WS] Updated sensorData for', sensorName, sensorData[sensorName])
      }
    } catch (e) {
      console.error('[WS] Error parsing sensor message:', e)
    }
  })

  // Subscribe to device state updates
  stompClient.subscribe('/topic/device-state', message => {
    try {
      const data = JSON.parse(message.body)
      console.log('[WS] Device state update:', data)
      // Handle both DEVICE_STATE_CHANGE and DEVICE_STATE_UPDATE formats
      const deviceId = data.deviceId || data.data?.deviceId
      const state = data.data?.state || data.data || {}
      if (deviceId) {
        deviceStates[deviceId] = {
          ...state,
          timestamp: data.timestamp,
          lastSeen: new Date().toISOString()
        }
        console.log('[WS] Updated deviceStates for', deviceId, deviceStates[deviceId])
      }
    } catch (e) {
      console.error('[WS] Error parsing device state message:', e)
    }
  })

  // Subscribe to device updates (general)
  stompClient.subscribe('/topic/device-updates', message => {
    try {
      const data = JSON.parse(message.body)
      console.log('[WS] Device update:', data)
    } catch (e) {
      console.error('[WS] Error parsing device update:', e)
    }
  })

  // Subscribe to scene events
  stompClient.subscribe('/topic/scenes', message => {
    try {
      const data = JSON.parse(message.body)
      
      const sceneEvent = {
        type: data.type,
        sceneId: data.data?.sceneId,
        sceneName: data.data?.sceneName,
        correlationId: data.data?.correlationId,
        devicesAffected: data.data?.devicesAffected || data.data?.lightsAffected,
        devicesConfirmed: data.data?.devicesConfirmed,
        latencyMs: data.data?.latencyMs,
        acksReceived: data.data?.acksReceived,
        lightsExpected: data.data?.lightsExpected,
        timestamp: data.timestamp
      }
      
      // Handle different scene event types
      switch (data.type) {
        case 'SCENE_PENDING':
          // Track pending command
          pendingScenes[sceneEvent.correlationId] = sceneEvent
          break
          
        case 'SCENE_CONFIRMED':
          // Remove from pending and mark as confirmed
          delete pendingScenes[sceneEvent.correlationId]
          lastSceneApplied.value = { ...sceneEvent, confirmed: true }
          break
          
        case 'SCENE_TIMEOUT':
          // Remove from pending and mark as timeout
          delete pendingScenes[sceneEvent.correlationId]
          lastSceneApplied.value = { ...sceneEvent, timedOut: true }
          break
          
        case 'SCENE_APPLIED':
          // Legacy event (without ack tracking)
          lastSceneApplied.value = sceneEvent
          break
      }
      
      // Keep last 10 scene events
      sceneEvents.value = [sceneEvent, ...sceneEvents.value.slice(0, 9)]
      
    } catch (e) {
      console.error('[WS] Error parsing scene message:', e)
    }
  })

  console.log('[WS] Subscribed to all topics')
}

/**
 * Disconnect WebSocket
 */
export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    connected.value = false
  }
}

/**
 * Get sensor data for a specific sensor
 */
export function getSensorData(sensorName) {
  return sensorData[sensorName] || null
}

/**
 * Get device state for a specific device
 */
export function getDeviceState(deviceId) {
  return deviceStates[deviceId] || null
}

/**
 * Clear last scene applied (for UI feedback dismissal)
 */
export function clearLastSceneApplied() {
  lastSceneApplied.value = null
}

/**
 * Check if a scene command is pending confirmation
 */
export function isScenePending(correlationId) {
  return !!pendingScenes[correlationId]
}

/**
 * Export reactive state for components
 */
export function useWebSocket() {
  return {
    connected,
    connectionError,
    sensorData,
    deviceStates,
    sceneEvents,
    lastSceneApplied,
    pendingScenes,
    connect: connectWebSocket,
    disconnect: disconnectWebSocket,
    getSensorData,
    getDeviceState,
    clearLastSceneApplied,
    isScenePending
  }
}
