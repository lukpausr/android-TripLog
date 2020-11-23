package com.dhbw.triplog.other

object Constants {
    const val TRIP_DATABASE_NAME = "trip_db"

    const val ACTION_START_RESUME_SERVICE = "ACTION_START_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    const val BACK_STACK_ROOT_TAG = "HomeFragment"


    // Notification Channel
    const val NOTIFICATION_CHANNEL_ID = "TRACKING_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME = "Tracking"
    const val NOTIFICATION_ID = 1

    // Location Tracking
    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_INTERVAL = 3000L

    // Permissions
    const val REQUEST_CODE_LOCATION_PERMISSION = 0

    // Shared Preferences
    const val SHARED_PREFERENCES_NAME = "sharedPref"
    const val KEY_DSGVO = "KEY_DSGVO"

}