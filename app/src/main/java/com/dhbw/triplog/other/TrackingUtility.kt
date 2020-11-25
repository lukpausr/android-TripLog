package com.dhbw.triplog.other

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
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

    fun getActivityAsString(result : ActivityTransitionResult) : String {
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

    fun getTransitionsToObserve() : List<ActivityTransition> {
        val transitions = mutableListOf<ActivityTransition>()
        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_FOOT)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        transitions +=
                ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        return transitions.toList()
    }

}