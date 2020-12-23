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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
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
import com.dhbw.triplog.other.Constants.SENSOR_UPDATE_INTERVAL
import com.dhbw.triplog.other.Constants.TIMER_UPDATE_INTERVAL
import com.dhbw.triplog.other.Constants.TRANSITION_RECEIVER_ACTION
import com.dhbw.triplog.other.SensorDatapoint
import com.dhbw.triplog.other.TrackingUtility
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService(), SensorEventListener {

    /*
    GPS DATA
     */
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var activityRecognitionClient: ActivityRecognitionClient

    private lateinit var activityRecognitionPendingIntent : PendingIntent

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var curNotificationBuilder: NotificationCompat.Builder

    private val tripTimeInSeconds = MutableLiveData<Long>()
    private var lastActivity = ""

    /*
    SENSOR DATA
     */
    private lateinit var sensorManager: SensorManager


    companion object {
        var allGpsPoints = mutableListOf<Location>()
        // Mutable LiveData object to enable modification by user
        val tripTimeInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val activityUpdates = MutableLiveData<String>()
        val gpsPoints = MutableLiveData<Location>()

        val accelerometerData = mutableListOf<SensorDatapoint>()
        val linearAccelerometerData = mutableListOf<SensorDatapoint>()
        val gyroscopeData = mutableListOf<SensorDatapoint>()
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder

        Timber.d("TRACKING_SERVICE: Entering TrackingService OnCreate")

        registerReceiver(activityReceiver, IntentFilter(TRANSITION_RECEIVER_ACTION))
        activityRecognitionPendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                Intent(TRANSITION_RECEIVER_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        setupSensor()

        isTracking.observe(this, Observer {
            Timber.d("TRACKING_SERVICE: isTracking changed to ${isTracking.value}")
            updateLocationTracking(it)
            registerForActivityUpdates(it)
        })

        activityUpdates.observe(this, Observer {
            //updateNotificationState(isTracking.value!!)
            activityChanged(it)
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
        unregisterSensors()
        pauseService()
        stopForeground(true)
        stopSelf()
    }

    private fun pauseService() {
        isTracking.postValue(false)
        unregisterSensors()
        isTimerEnabled = false
    }


    /*
    Sensor
     */

    private fun setupSensor() {
        // Return reference to sensorService
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun registerSensors() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SENSOR_UPDATE_INTERVAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also {
            sensorManager.registerListener(this, it, SENSOR_UPDATE_INTERVAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SENSOR_UPDATE_INTERVAL)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val datapoint = SensorDatapoint(event)
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> accelerometerData.add(datapoint)
                Sensor.TYPE_LINEAR_ACCELERATION -> linearAccelerometerData.add(datapoint)
                Sensor.TYPE_GYROSCOPE -> gyroscopeData.add(datapoint)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {   }


    /*
    GPS
     */

    private fun addTrackPoint(location: Location?) {
        if (location != null) {
            gpsPoints.postValue(location)
        }
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
    private val locationCallback = object : LocationCallback() {
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
            val transitions = TrackingUtility.getTransitionsToObserve()
            val request = ActivityTransitionRequest(transitions)

//            activityRecognitionClient.requestActivityTransitionUpdates(
//                    request,
//                    activityRecognitionPendingIntent
//            )
//                    .addOnSuccessListener { Timber.d("TRACKING_SERVICE: Activity Recognition successfully set up") }

            activityRecognitionClient.requestActivityUpdates(
                    0,
                    activityRecognitionPendingIntent
            )
                    .addOnSuccessListener { Timber.d("TRACKING_SERVICE: Activity Update Request successfully set up") }

        } else {
            Timber.d("TRACKING_SERVICE: Deregister from Activity Updates")
            activityRecognitionClient.removeActivityTransitionUpdates(activityRecognitionPendingIntent)
            activityRecognitionClient.removeActivityUpdates(activityRecognitionPendingIntent)
        }

    }

    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("TRACKING_SERVICE: Entered onReceive")
            if(ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)!!
//                for (event in result.transitionEvents) {
//                    Timber.d("TRACKING_SERVICE: Event received")
//                }
                activityUpdates.postValue(TrackingUtility.getActivityAsString(result))
                Timber.d("TRACKING_SERVICE: ${TrackingUtility.getActivityAsString(result)}")
            }
            if(ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)!!
                activityUpdates.postValue(result.mostProbableActivity.toString())
                Timber.d("TRACKING_SERVICE: ${result.mostProbableActivity}")
            }
        }
    }

    private fun activityChanged(curActivity: String) {
        if(lastActivity != curActivity) {
            isTimerEnabled = false
            lastActivity = curActivity
            startTimer()
        }
    }

    private var timeStarted = 0L
    private var tripTime = 0L
    private var lastSecondTimestamp = 0L
    private var isTimerEnabled = false

    private fun startTimer() {
        isTimerEnabled = true

        lastSecondTimestamp = 0L
        tripTime = 0L
        tripTimeInSeconds.postValue(0L)
        tripTimeInMillis.postValue(0L)

        isTracking.postValue(true)

        timeStarted = System.currentTimeMillis()

        CoroutineScope(Dispatchers.Main).launch {
            while(isTimerEnabled) {
                tripTime = System.currentTimeMillis() - timeStarted
                tripTimeInMillis.postValue(tripTime)
                if(tripTimeInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    tripTimeInSeconds.postValue(tripTimeInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            cancel()
        }
    }

    private fun updateNotificationState(isTracking: Boolean) {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isTracking) {
            curNotificationBuilder = baseNotificationBuilder
                    .setContentText(activityUpdates.value?.toString())
            notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
        }
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

        tripTimeInSeconds.observe(this, Observer {
            if(isTracking.value!!) {
                val notification = curNotificationBuilder
                        .setStyle(NotificationCompat.BigTextStyle().bigText("${lastActivity}\nTime spent: ${TrackingUtility.getFormattedStopWatchTime(it * 1000L)}"))
                        .setContentText("${lastActivity}\nTime spent: ${TrackingUtility.getFormattedStopWatchTime(it * 1000L)}")
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
        gpsPoints.observe(this, Observer {
            allGpsPoints.add(it)
        })

        registerSensors()

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