package com.dhbw.triplog.other

import android.graphics.Color

/**
 * Constant class containing all relevant constants for the TripLog project
 */
object Constants {
    // Database
    const val TRIP_DATABASE_NAME = "trip_db"

    // Foreground Service Intent Messages
    const val ACTION_START_RESUME_SERVICE = "ACTION_START_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    // Navigation Custom Back Stack Root Tag to forbid navigation back to the setup fragment
    const val BACK_STACK_ROOT_TAG = "HomeFragment"

    // Foreground Service Intent
    const val ACTION_SHOW_TRIP_FRAGMENT = "ACTION_SHOW_TRIP_FRAGMENT"

    // Notification Channel
    const val NOTIFICATION_CHANNEL_ID = "TRACKING_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME = "Tracking"
    const val NOTIFICATION_ID = 1

    // Location Tracking
    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_INTERVAL = 3000L

    // Sensor Update Interval uS
    const val SENSOR_UPDATE_INTERVAL = 10000
    const val SENSOR_TYPE_ACCELEROMETER = "SENSOR_TYPE_ACCELEROMETER"
    const val SENSOR_TYPE_LINEAR_ACCELEROMETER = "SENSOR_TYPE_LINEAR_ACCELEROMETER"
    const val SENSOR_TYPE_GYROSCOPE = "SENSOR_TYPE_GYROSCOPE"

    // Activity Recognition
    const val TRANSITION_RECEIVER_ACTION = "TRANSITION_RECEIVER_ACTION"

    // Map
    const val MAP_ZOOM = 15f
    const val POLYLINE_COLOR = Color.RED
    const val POLYLINE_WIDTH = 8f

    // Timer COROUTINE
    const val TIMER_UPDATE_INTERVAL = 50L

    // Permissions
    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    // Shared Preferences
    const val SHARED_PREFERENCES_NAME = "sharedPref"
    const val KEY_DSGVO = "KEY_DSGVO"
    const val KEY_SELECTED_LABEL = "KEY_SELECTED_LABEL"

    // Random Unique User ID
    const val DEVICE_ID = "DEVICE_ID"
}