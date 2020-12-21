package com.dhbw.triplog.other

import android.content.SharedPreferences
import com.dhbw.triplog.other.Constants.DEVICE_ID
import timber.log.Timber
import java.util.*

object DeviceRandomUUID {

    fun createRUUID (sharedPref : SharedPreferences) {
        val uuid = UUID.randomUUID().toString()
        sharedPref.edit().putString(DEVICE_ID, uuid).apply()
        Timber.d("Random generated UUID: %s", uuid)
    }

    fun getRUUID(sharedPref : SharedPreferences) : String {
        val uuid = sharedPref.getString(DEVICE_ID, null) ?: return "ID_NULL_ERROR"
        return uuid
    }

}