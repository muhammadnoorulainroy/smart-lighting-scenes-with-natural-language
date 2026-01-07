package com.smartlighting.mobile.util

object Constants {
    const val BASE_URL = "http://192.168.1.46:8080/"
    
    /**
     * API Endpoints
     */
    object Endpoints {
        const val ROOMS = "api/rooms"
        const val SCENES = "api/scenes"
        const val LIGHTING = "api/lighting"
        const val SCHEDULES = "api/schedules"
        const val NLP = "api/nlp"
        const val AUTH = "api/auth"
    }
    
    /**
     * Network timeout settings (in seconds)
     */
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    
    /**
     * SharedPreferences keys
     */
    object PrefsKeys {
        const val API_URL = "api_url"
        const val LAST_SELECTED_ROOM = "last_selected_room"
    }
    
    /**
     * Speech recognition constants
     */
    object Speech {
        const val RECOGNITION_LANGUAGE = "en-US"
        const val MAX_RESULTS = 1
    }
}

