package com.dhbw.triplog.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
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
import com.dhbw.triplog.other.DataUtility
import com.dhbw.triplog.other.Math
import com.dhbw.triplog.other.SensorDatapoint
import com.dhbw.triplog.other.TrackingUtility
import com.github.doyaaaaaken.kotlincsv.client.CsvFileWriter
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * TrackingService class, used for running the GPS and Sensor data collection as foreground
 * activity
 *
 * @property fusedLocationProviderClient Google Fused Location API
 * @property sensorManager Android Sensor Manager
 * @property baseNotificationBuilder NotificationBuilder
 * @property curNotificationBuilder Second NotificationBuilder, being used for updates
 * @property tripTimeInSeconds Total trip time in seconds for the current recording
 */
@AndroidEntryPoint
class TrackingService : LifecycleService(), SensorEventListener {

    /**
     * Variables being used for GPS Tracking
     */
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    /**
     * Variables being used for Sensor Data Tracking
     */
    private lateinit var sensorManager: SensorManager

    /**
     * Variables used for foreground activity
     */
    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    private lateinit var curNotificationBuilder: NotificationCompat.Builder
    private val tripTimeInSeconds = MutableLiveData<Long>()

    companion object {
        // List with all GPS Points
        var allGpsPoints = mutableListOf<Location>()
        // Mutable LiveData objects to enable modification by user and ability to Observe
        val tripTimeInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val gpsPoints = MutableLiveData<Location>()
        // List of Sensor Data Objects
        val accelerometerData = mutableListOf<SensorDatapoint>()
        val linearAccelerometerData = mutableListOf<SensorDatapoint>()
        val gyroscopeData = mutableListOf<SensorDatapoint>()
    }

    /**
     * Being used to setup the sensor and to observe MutableLiveData isTracking,
     * controlling if the Location is currently being tracked when the TrackingService is
     * initially set up
     */
    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder

        setupSensor()

        isTracking.observe(this, Observer {
            Timber.d("TRACKING_SERVICE: isTracking changed to ${isTracking.value}")
            updateLocationTracking(it)
            updateNotificationState(it)
        })
    }

    /**
     * Controls the Service class behaviour based on the
     * received intent
     */
    @KotlinCsvExperimental
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

    /**
     * Stop service by unregistering the Sensors, pausing the service and stopping the
     * foreground service and finally stopping the service itself
     */
    @KotlinCsvExperimental
    private fun stopService() {
        isTracking.postValue(false)

        parallelWriteSensorData()
        writerSensor.close()
        writerGPS.close()
        firstWriteOperation = true

        pauseService()
        stopForeground(true)
        stopSelf()
    }

    /**
     * Pause service by posting 'false' to the observed MutableLiveData object 'isTracking',
     * unregistering the sensors and disabling the Timer
     */
    private fun pauseService() {
        isTracking.postValue(false)
        unregisterSensors()
        isTimerEnabled = false
    }

    /**
     * SENSOR RELATED METHODS
     */

    /**
     * Setting up the sensor by creating a reference to sensorService for the
     * 'lateinit var sensorManager'
     */
    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    /**
     * Registering listeners to sensors for 'sensorManager' of type:
     * ACCELEROMETER, LINEAR_ACCELERATION and GYROSCOPE
     */
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

    /**
     * Unregistering the sensors by deleting all listeners setup for 'sensorManager'
     */
    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Overriding onSensorChanged to save the sensor output, each time a onSensorChanged
     * event is being called.
     * Because the event parameter is pointing to the SensorEvent and the SensorEvent is overridden
     * with each onSensorChanged call, we need to copy the SensorEvent to have persistent data. The
     * solution for this is to create a custom class 'SensorDatapoint', which contains all
     * properties of 'SensorEvent' we need to use later on
     *
     * @param event SensorEvent object, which is always the same (being pointed to!)
     */
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

    /**
     * Overriding because of interface implementation
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {   }

    /**
     * GPS RELATED METHODS
     */

    /**
     * Adding a GPS Point to the Mutable LiveData Object 'gpsPoints' which can be observed
     * by the TripFragment to show the path on Google Maps
     *
     * @param location Location Object containing information about a recorded GPS Point
     */
    private fun addTrackPoint(location: Location?) {
        if (location != null) {
            gpsPoints.postValue(location)
        }
    }

    /**
     * updates the location tracking to receive location Updates
     * Suppress is possible because permissions are already checked and handled by EasyPermissions
     *
     * @param isTracking Information about the current tracking state
     */
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

    /**
     * Anonymous inner class with overridden onLocationResult functionality
     */
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

    /**
     * FOREGROUND SERVICE RELATED METHODS
     */

    private var timeStarted = 0L
    private var tripTime = 0L
    private var lastSecondTimestamp = 0L
    private var isTimerEnabled = false

    /**
     * Timer functionality, once started, the timer continues to count in a coroutine and posts
     * updates every second in 'tripTimeInSeconds' MutableLiveData Object
     * The Timer can be stopped by setting 'isTimerEnabled' to true
     * When starting the timer, 'isTracking' will be automatically set to true
     */
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

    /**
     * Starting the foreground service by setting 'isTracking' to true, starting the timer and
     * creating the necessary notification channel
     * Within this call, tripTimeInSeconds will be observed to update the notification element and
     * to save each collected GPS Point in a MutableList 'allGpsPoints'
     */
    @KotlinCsvExperimental
    private fun startForegroundService() {
        // Starting the actual tracking / logging
        isTracking.postValue(true)
        firstWriteOperation = true
        startTimer()
        registerSensors()

        // Get Notification Manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create NotificationChannel if Android Version = 8+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        // Start Foreground Service
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        // Observe LiveData Objects
        tripTimeInSeconds.observe(this, Observer {
            if(isTracking.value!!) {
                val notification = curNotificationBuilder
                        .setContentText("Recording - ${TrackingUtility.getFormattedStopWatchTime(it * 1000L)}")
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
            if((it.toInt() % 10) == 0) {
                parallelWriteSensorData()
            }
        })
        gpsPoints.observe(this, Observer {
            allGpsPoints.add(it)
        })
    }

    /**
     * Update Notification when LiveData isTracking indicating the current tracking state is true
     *
     * @param isTracking Indication of the current tracking state
     */
    private fun updateNotificationState(isTracking: Boolean) {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isTracking) {
            curNotificationBuilder = baseNotificationBuilder
            notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
        }
    }

    /**
     * Create a notification channel which is being needed to create a foreground activity
     * on Android Oreo and higher
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private var firstWriteOperation = true

    private lateinit var writerSensor: CsvFileWriter
    private lateinit var writerGPS: CsvFileWriter
    private var currentPositionInSensorArray = 0
    private var destinationPositionInSensorArray = 0
    private var currentPositionInGPSArray = 0
    private var destinationPositionInGPSArray = 0

    private val simpleDateFormat =
            SimpleDateFormat("yyyy:MM:dd:HH:mm:ss:SS:z", Locale.getDefault())

    /**
     * Parallel to tracking, writing Sensor and GPS Data to a temporary csv file to save time
     * To write the csv data, kotlin-csv is being used:
     * https://github.com/doyaaaaaken/kotlin-csv
     */
    @KotlinCsvExperimental
    private fun parallelWriteSensorData() {

        // Define the file path to temp_sensor, if it is the first write operation
        if(firstWriteOperation) {
            val path = this.filesDir.toString()
            val filePathSensor = "$path/temp_sensor.csv"
            val filePathGPS = "$path/temp_gps.csv"
            writerSensor = csvWriter().openAndGetRawWriter(filePathSensor)
            writerSensor.writeRow(
                    "Time_in_ns", "ACC_X", "ACC_Y", "ACC_Z",
                    "Time_in_ns", "LINEAR_ACC_X", "LINEAR_ACC_Y", "LINEAR_ACC_Z",
                    "Time_in_ns", "w_X", "w_Y", "w_Z"
            )
            writerGPS = csvWriter().openAndGetRawWriter(filePathGPS)
            writerGPS. writeRow(
                    "Timestamp", "Time_in_s", "Latitude", "Longitude", "Altitude", "Speed"
            )
            firstWriteOperation = false
        }


        // Fill the temp_sensor file during data collection
        destinationPositionInSensorArray = Math.min(
                accelerometerData.size,
                linearAccelerometerData.size,
                gyroscopeData.size
        )
        if(destinationPositionInSensorArray != 0) {
            destinationPositionInSensorArray -= 1
        }
        for (i in currentPositionInSensorArray until destinationPositionInSensorArray) {
            Timber.tag("CSV_WRITER_SENSOR").d("Current Position: $i")
            writerSensor.writeRow(
                    DataUtility.convertEvent(accelerometerData.getOrNull(i))
                            + DataUtility.convertEvent(linearAccelerometerData.getOrNull(i))
                            + DataUtility.convertEvent(gyroscopeData.getOrNull(i))
            )
        }
        currentPositionInSensorArray = destinationPositionInSensorArray


        // Fill the temp_gps file during data collection
        destinationPositionInGPSArray = allGpsPoints.size
        if(destinationPositionInGPSArray != 0) {
            destinationPositionInGPSArray -= 1
        }
        for (i in currentPositionInGPSArray until destinationPositionInGPSArray) {
            Timber.tag("CSV_WRITER_GPS").d("Current Position: $i")
            writerGPS.writeRow(
                    simpleDateFormat.format(allGpsPoints[i].time),
                    (allGpsPoints[i].time / 1000).toString(),
                    allGpsPoints[i].latitude.toString(),
                    allGpsPoints[i].longitude.toString(),
                    allGpsPoints[i].altitude.toString(),
                    allGpsPoints[i].speed.toString()
            )
        }
        currentPositionInGPSArray = destinationPositionInGPSArray

    }

}