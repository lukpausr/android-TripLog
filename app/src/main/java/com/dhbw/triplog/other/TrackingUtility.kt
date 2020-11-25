package com.dhbw.triplog.other

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.location.ActivityTransitionResult
import pub.devrel.easypermissions.EasyPermissions

object TrackingUtility {

    fun hasLocationPermissions(context: Context) =
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                EasyPermissions.hasPermissions(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                EasyPermissions.hasPermissions(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION
                )
            }

    fun decryptActivity(result : ActivityTransitionResult) : String {
        var activity = ""
        when (result.transitionEvents.last().activityType) {
            0 -> activity = "IN_VEHICLE"
            1 -> activity = "ON_BICYCLE"
            2 -> activity = "ON_FOOT"
            3 -> activity = "STILL"
            4 -> activity = "UNKNOWN"
            5 -> activity = "TILTING"
            7 -> activity = "WALKING"
            8 -> activity = "RUNNING"
        }
        return activity
    }

}