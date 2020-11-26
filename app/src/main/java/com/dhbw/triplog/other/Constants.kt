package com.dhbw.triplog.other

object Constants {
    const val TRIP_DATABASE_NAME = "trip_db"

    const val ACTION_START_RESUME_SERVICE = "ACTION_START_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

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

    // Activity Recognition
    const val TRANSITION_RECEIVER_ACTION = "TRANSITION_RECEIVER_ACTION"

    // Timer COROUTINE
    const val TIMER_UPDATE_INTERVAL = 50L

    // Permissions
    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    // Shared Preferences
    const val SHARED_PREFERENCES_NAME = "sharedPref"
    const val KEY_DSGVO = "KEY_DSGVO"
    const val KEY_TRACKING_STATE = "KEY_TRACKING_STATE"

}