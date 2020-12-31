package com.dhbw.triplog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base application class for maintaining global application state.
 * Custom implementation to enable planting of Timber Debug Tree. This class must be referenced to
 * in AndroidManifest.xml as android:name=".BaseApplication"
 */
@HiltAndroidApp
class BaseApplication : Application() {

    /**
     * Overriding onCreate to plant the Debug Tree here for debugging purposes
     */
    override fun onCreate() {
        super.onCreate()
        // Timber.plant(Timber.DebugTree())  // Debug Logging
    }
}