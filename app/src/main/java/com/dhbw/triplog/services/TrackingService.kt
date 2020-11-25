package com.dhbw.triplog.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.dhbw.triplog.other.Constants.ACTION_PAUSE_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.FASTEST_LOCATION_INTERVAL
import com.dhbw.triplog.other.Constants.LOCATION_UPDATE_INTERVAL
import com.dhbw.triplog.other.Constants.NOTIFICATION_CHANNEL_ID
import com.dhbw.triplog.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.dhbw.triplog.other.Constants.NOTIFICATION_ID
import com.dhbw.triplog.other.Constants.TRANSITION_RECEIVER_ACTION
import com.dhbw.triplog.other.TrackingUtility
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var activityRecognitionClient: ActivityRecognitionClient

    lateinit var activityRecognitionPendingIntent : PendingIntent

    //var activityReceiver = ActivityReceiver()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private val transitions = mutableListOf<ActivityTransition>()

    companion object {
        // Mutable LiveData object to enable modification by user
        val isTracking = MutableLiveData<Boolean>()
        val activityUpdates = MutableLiveData<String>()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("TRACKING_SERVICE: Entering TrackingService OnCreate")

        registerReceiver(activityReceiver, IntentFilter(TRANSITION_RECEIVER_ACTION))
        activityRecognitionPendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                Intent(TRANSITION_RECEIVER_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        isTracking.observe(this, Observer {
            Timber.d("TRACKING_SERVICE: isTracking changed to ${isTracking.value}")
            updateLocationTracking(it)
            registerForActivityUpdates(it)
        })

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_RESUME_SERVICE -> {
                    Timber.d("TRACKING_SERVICE: Started / Resumed Service")
                    startForegroundService()
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("TRACKING_SERVICE: Paused Service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("TRACKING_SERVICE: Stopped Service")
                    stopService()
                }
                else -> Timber.d("TRACKING_SERVICE: ERROR")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopService() {
        unregisterReceiver(activityReceiver)
        pauseService()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        isTracking.postValue(false)
    }

    private fun addTrackPoint(location: Location?) {

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
                        Timber.d("NEW LOCATION: [Lon: ${location.latitude}, Lat: ${location.longitude}, Alt: ${location.altitude}]")
                    }
                }
            }
        }
    }

    private fun registerForActivityUpdates(isTracking: Boolean) {
        if(isTracking) {
            Timber.d("TRACKING_SERVICE: Register for Activity Updates")
            addObservedTransitionTypes()
            val request = ActivityTransitionRequest(transitions)

//            activityRecognitionClient.requestActivityTransitionUpdates(
//                    request,
//                    activityRecognitionPendingIntent
//            )
//                    .addOnSuccessListener {
//                        Timber.d("Activity Recognition successfully set up")
//                    }

            activityRecognitionClient.requestActivityUpdates(
                    0,
                    activityRecognitionPendingIntent
            )
                    .addOnSuccessListener { Timber.d("TRACKING_SERVICE: Activity Update Request Successfull") }


        } else {
            Timber.d("TRACKING_SERVICE: Deregister from Activity Updates")
            activityRecognitionClient.removeActivityTransitionUpdates(activityRecognitionPendingIntent)
            activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent)
        }

    }

    val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("TRACKING_SERVICE: Entered onReceive")
            if(ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)!!
//                for (event in result.transitionEvents) {
//                    Timber.d("TRACKING_SERVICE: Event received")
//                }
                activityUpdates.postValue(TrackingUtility.decryptActivity(result))
                Timber.d("TRACKING_SERVICE: ${TrackingUtility.decryptActivity(result)}")
            }
            if(ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)!!
                activityUpdates.postValue(result.mostProbableActivity.toString())
                Timber.d("TRACKING_SERVICE: ${result.mostProbableActivity.toString()}")
            }
        }
    }

    private fun addObservedTransitionTypes() {
        transitions.clear()
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
    }

    private fun startForegroundService() {
        isTracking.postValue(true)
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