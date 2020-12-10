package com.dhbw.triplog.other

import android.location.Location
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat

object DataExportUtility {

    fun writeGPSDataToFile(file: File, gpsPoints: MutableList<Location>, selectedTransportType: Labels?) {

        val labels = labelsToString(selectedTransportType)

        val filename =  file.absolutePath +
                        "/" +
                        System.currentTimeMillis().toString() + labels

        Log.d("filename", filename)

        val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss:SS:z")

        csvWriter().open("$filename.csv") {
            writeRow("Time", "Time_in_s", "Latitude", "Longitude", "Altitude", "Speed")
            for (gpsPoint in gpsPoints) {
                writeRow(
                        simpleDateFormat.format(gpsPoint.time),
                        (gpsPoint.time/1000).toString(),
                        gpsPoint.latitude.toString(),
                        gpsPoint.longitude.toString(),
                        gpsPoint.altitude.toString(),
                        gpsPoint.speed.toString()
                )
            }
        }

    }

    private fun labelsToString(selectedTransportType: Labels?) : String {
        val stringBuilder = StringBuilder()
        if(selectedTransportType?.label != "") {
            stringBuilder.append("_" + selectedTransportType?.label)
        }
        if(selectedTransportType?.subLabel != "") {
            stringBuilder.append("_" + selectedTransportType?.subLabel)
        }
        if(selectedTransportType?.subSubLabel != "") {
            stringBuilder.append("_" + selectedTransportType?.subSubLabel)
        }
        return stringBuilder.toString()
    }



}