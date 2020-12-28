package com.dhbw.triplog.other

import android.hardware.SensorEvent

/*
As the SensorEvent is reused for each Sensor Datapoint, we need to clone the data for later use
https://stackoverflow.com/a/21180490
https://stackoverflow.com/a/14930068
 */

/**
 * As the SensorEvent is reused for each Sensor Datapoint,
 * we need to clone the data for later use and create an Object of type 'SensorDatapoint' to do so.
 * Additional information can be found at:
 * https://stackoverflow.com/a/21180490
 * https://stackoverflow.com/a/14930068
 *
 * @param event onSensorChanged event of type SensorEvent (sensorManager)
 *
 * @property timestamp Timestamp, the sensor values were recorded at
 * @property values Sensor values as list of 3 values (x-, y-, z- Axis)
 * @property sensor Sensor information
 */
class SensorDatapoint (private val event: SensorEvent) {
    val timestamp = event.timestamp
    val values = listOf<Float?>(
            event.values.getOrNull(0),
            event.values.getOrNull(1),
            event.values.getOrNull(2)
    )
    val sensor = event.sensor

    /**
     * Overridden toString method for better debug visibility
     *
     * @return String containing the Timestamp and Sensor values
     */
    override fun toString(): String {
        return "Timestamp: $timestamp Values: $values"
    }

}