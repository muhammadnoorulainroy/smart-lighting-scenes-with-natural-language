package com.smartlighting.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartLightingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide services here
    }
}
