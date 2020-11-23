package com.dhbw.triplog.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.dhbw.triplog.other.Constants.ACTION_PAUSE_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.FASTEST_LOCATION_INTERVAL
import com.dhbw.triplog.other.Constants.LOCATION_UPDATE_INTERVAL
import com.dhbw.triplog.other.Constants.NOTIFICATION_CHANNEL_ID
import com.dhbw.triplog.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.dhbw.triplog.other.Constants.NOTIFICATION_ID
import com.dhbw.triplog.other.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import timber.log.Timber
import javax.inject.Inject

class TrackingService : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    companion object {
        // Mutable LiveData object to enable modification by user
        val isTracking = MutableLiveData<Boolean>()
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {

                ACTION_START_RESUME_SERVICE -> {
                    Timber.d("Started / Resumed Service")
                    startForegroundService()
                }

                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused Service")
                    pauseService()
                }

                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped Service")
                    stopService()
                }

                else -> Timber.d("ERROR")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopService() {
        pauseService()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        isTracking.postValue(false)
    }

    private fun addTrackPoint(location: Location?) {
        TODO("Not yet implemented")
    }

    // Suppress possible because permissions are already checked and handled by EasyPermissions
    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if(isTracking) {
            if(TrackingUtility.hasLocationPermissions(this)) {

                // Service Parameters for requests to FusedLocationProviderAPI
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }

                // Requests location updates with a callback on the specified Looper thread
                fusedLocationProviderClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()  // Message queue
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    // Anonymous inner class with overridden onLocationResult functionality
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {
                        addTrackPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun startForegroundService() {

        // Get Notification Manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create NotificationChannel if Android Version = 8+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // Start Foreground Service
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


}