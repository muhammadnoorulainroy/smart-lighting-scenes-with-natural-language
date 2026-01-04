/**
 * @fileoverview WebSocket store for real-time device state updates.
 *
 * Manages STOMP over WebSocket connection to the backend for:
 * - Live sensor data (temperature, humidity, light level)
 * - Device state changes (on/off, brightness, color)
 * - Scene application events and acknowledgments
 *
 * Uses SockJS for WebSocket transport with automatic reconnection.
 *
 * @module stores/websocket
 */

import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

// Development Logger

/** Whether we're in development mode */
const isDev = import.meta.env.DEV

/**
 * Development-only logger. No-op in production.
 * @param {...*} args - Log arguments
 */
const log = isDev ? (...args) => console.log('[WS]', ...args) : () => {}

/**
 * Error logger. Always active (errors are important).
 * @param {...*} args - Error arguments
 */
const logError = (...args) => console.error('[WS]', ...args)

// Reactive State

/**
 * Sensor readings by sensor name.
 * Updated in real-time via WebSocket.
 * @type {Object}
 */
const sensorData = reactive({})

/**
 * Device states by device ID.
 * Contains isOn, brightness, color, lastSeen, etc.
 * @type {Object}
 */
const deviceStates = reactive({})

/**
 * Recent scene events (last 10).
 * Used for activity feed and notifications.
 */
const sceneEvents = ref([])

/**
 * Last scene that was applied (for toast notifications).
 * Includes confirmation status from ESP32 ACK.
 */
const lastSceneApplied = ref(null)

/**
 * Scenes currently pending acknowledgment.
 * Keyed by correlationId.
 * @type {Object}
 */
const pendingScenes = reactive({})

/** WebSocket connection status */
const connected = ref(false)

/** Last connection error message */
const connectionError = ref(null)

/** STOMP client instance */
let stompClient = null

// Connection Management

/**
 * Establishes WebSocket connection to the backend.
 *
 * Uses SockJS transport with STOMP protocol.
 * Automatically reconnects on disconnect (5s delay).
 * Subscribes to all relevant topics on connect.
 *
 * @returns {void}
 */
export function connectWebSocket() {
  if (stompClient && connected.value) {
    log('Already connected')
    return
  }

  const wsUrl = `${import.meta.env.VITE_API_URL ?? ''}/ws`
  log('Connecting to:', wsUrl)

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: isDev ? str => console.log('[STOMP]', str) : () => {},
    onConnect: () => {
      log('Connected!')
      connected.value = true
      connectionError.value = null
      subscribeToTopics()
    },
    onDisconnect: () => {
      log('Disconnected')
      connected.value = false
    },
    onStompError: frame => {
      logError('STOMP error:', frame)
      connectionError.value = frame.headers?.message || 'Connection error'
    },
    onWebSocketError: event => {
      logError('WebSocket error:', event)
      connectionError.value = 'WebSocket connection failed'
    }
  })

  stompClient.activate()
}

/**
 * Subscribes to all WebSocket topics.
 *
 * Topics:
 * - /topic/sensors: Environmental sensor readings
 * - /topic/device-state: LED state changes
 * - /topic/device-updates: General device updates
 * - /topic/scenes: Scene application events and ACKs
 *
 * @private
 */
function subscribeToTopics() {
  if (!stompClient || !stompClient.connected) {
    return
  }

  // Subscribe to sensor updates
  stompClient.subscribe('/topic/sensors', message => {
    try {
      const data = JSON.parse(message.body)
      log('Sensor update:', data)
      const sensorName = data.data?.sensorName
      const readings = data.data?.readings || {}
      if (sensorName) {
        sensorData[sensorName] = {
          ...readings,
          timestamp: data.timestamp
        }
        log('Updated sensorData for', sensorName, sensorData[sensorName])
      }
    } catch (e) {
      logError('Error parsing sensor message:', e)
    }
  })

  // Subscribe to device state updates
  stompClient.subscribe('/topic/device-state', message => {
    try {
      const data = JSON.parse(message.body)
      log('Device state update:', data)
      const deviceId = data.deviceId || data.data?.deviceId
      const state = data.data?.state || data.data || {}
      if (deviceId) {
        deviceStates[deviceId] = {
          ...state,
          timestamp: data.timestamp,
          lastSeen: new Date().toISOString()
        }
        log('Updated deviceStates for', deviceId, deviceStates[deviceId])
      }
    } catch (e) {
      logError('Error parsing device state message:', e)
    }
  })

  // Subscribe to device updates (general)
  stompClient.subscribe('/topic/device-updates', message => {
    try {
      const data = JSON.parse(message.body)
      log('Device update:', data)
    } catch (e) {
      logError('Error parsing device update:', e)
    }
  })

  // Subscribe to scene events (including ACK tracking)
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
          // Track pending command awaiting ACK
          pendingScenes[sceneEvent.correlationId] = sceneEvent
          break

        case 'SCENE_CONFIRMED':
          // All ACKs received - command successful
          delete pendingScenes[sceneEvent.correlationId]
          lastSceneApplied.value = { ...sceneEvent, confirmed: true }
          break

        case 'SCENE_TIMEOUT':
          // Not all ACKs received within timeout
          delete pendingScenes[sceneEvent.correlationId]
          lastSceneApplied.value = { ...sceneEvent, timedOut: true }
          break

        case 'SCENE_APPLIED':
          // Legacy event (without ACK tracking)
          lastSceneApplied.value = sceneEvent
          break
      }

      // Keep last 10 scene events for activity feed
      sceneEvents.value = [sceneEvent, ...sceneEvents.value.slice(0, 9)]
    } catch (e) {
      logError('Error parsing scene message:', e)
    }
  })

  log('Subscribed to all topics')
}

/**
 * Disconnects the WebSocket connection.
 *
 * Should be called when the app unmounts or user logs out.
 *
 * @returns {void}
 */
export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    connected.value = false
  }
}

// Data Access Functions

/**
 * Gets sensor readings for a specific sensor.
 *
 * @param {string} sensorName - Sensor identifier
 * @returns {Object|null} Sensor readings or null if not available
 */
export function getSensorData(sensorName) {
  return sensorData[sensorName] || null
}

/**
 * Gets the current state of a device.
 *
 * @param {string} deviceId - Device UUID
 * @returns {Object|null} Device state or null if not available
 */
export function getDeviceState(deviceId) {
  return deviceStates[deviceId] || null
}

/**
 * Clears the last scene applied notification.
 *
 * Call this after displaying a toast to dismiss it.
 *
 * @returns {void}
 */
export function clearLastSceneApplied() {
  lastSceneApplied.value = null
}

/**
 * Checks if a scene command is pending acknowledgment.
 *
 * @param {string} correlationId - Command correlation ID
 * @returns {boolean} True if still pending
 */
export function isScenePending(correlationId) {
  return !!pendingScenes[correlationId]
}

/**
 * Composable function to access WebSocket state and methods.
 *
 * Provides reactive access to connection status, device states,
 * and scene events for use in Vue components.
 *
 * @returns {Object} WebSocket state and methods
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
