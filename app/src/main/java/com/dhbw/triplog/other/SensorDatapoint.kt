package com.dhbw.triplog.other

import android.hardware.Sensor
import android.hardware.SensorEvent

/*
As the SensorEvent is reused for each Sensor Datapoint, we need to clone the data for later use
https://stackoverflow.com/a/21180490
https://stackoverflow.com/a/14930068
 */

class SensorDatapoint (private val event: SensorEvent) {
    val timestamp = event.timestamp
    val values = listOf<Float?>(
            event.values.getOrNull(0),
            event.values.getOrNull(1),
            event.values.getOrNull(2)
    )
    val sensor = event.sensor

    override fun toString(): String {
        return "Timestamp: $timestamp Values: $values Sensor: $sensor"
    }

}