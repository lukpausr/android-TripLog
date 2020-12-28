package com.dhbw.triplog.other

import android.content.SharedPreferences
import com.dhbw.triplog.other.Constants.DEVICE_ID
import timber.log.Timber
import java.util.*

/**
 * Static class like implementation of methods regarding creating, storing and returning a
 * random unique user id, which is being used to upload the collected data in a user specific
 * folder in the Google Firebase Storage instance
 */
object DeviceRandomUUID {

    /**
     * Creates a random unique user ID and persistently stores it in Shared Preferences as key-
     * value pair
     *
     * @param sharedPref Shared Preferences reference, being used to persistently store the UUID
     */
    fun createRUUID (sharedPref : SharedPreferences) {
        val uuid = UUID.randomUUID().toString()
        sharedPref.edit().putString(DEVICE_ID, uuid).apply()
        Timber.d("Random generated UUID: %s", uuid)
    }

    /**
     * Returns the random unique user ID by searching Shared Preferences for the entry DEVICE_ID
     *
     * @param sharedPref Shared Preferences reference, being used to retrieve the UUID
     *
     * @return String containing the UUID
     */
    fun getRUUID(sharedPref : SharedPreferences) : String {
        return sharedPref.getString(DEVICE_ID, null) ?: return "ID_NULL_ERROR"
    }

}