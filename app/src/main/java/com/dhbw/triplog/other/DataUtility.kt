package com.dhbw.triplog.other

import android.content.Context
import android.hardware.SensorEvent
import android.location.Location
import android.net.Uri
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import com.google.gson.Gson
import timber.log.Timber
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

object DataUtility {

    fun getPathAndFilename(context: Context, selectedTransportType: Labels?, timestamp: Long) : String {
        val path = context.filesDir.toString()
        val labels = labelsToString(selectedTransportType)
        return "$path/$timestamp$labels"
    }

    fun getFormattedDate(timestamp: Long) : String {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(timestamp)
    }

    /*
    https://github.com/doyaaaaaken/kotlin-csv
     */
    fun writeGPSDataToFile(
            path: String,
            gpsPoints: MutableList<Location>
    ) : String {
        val filePath = path + "_GPS"
        Timber.d("$filePath.csv")

        val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss:SS:z", Locale.getDefault())
        csvWriter().open("$filePath.csv") {
            writeRow("Timestamp", "Time_in_s", "Latitude", "Longitude", "Altitude", "Speed")
            for (gpsPoint in gpsPoints) {
                writeRow(
                        simpleDateFormat.format(gpsPoint.time),
                        (gpsPoint.time / 1000).toString(),
                        gpsPoint.latitude.toString(),
                        gpsPoint.longitude.toString(),
                        gpsPoint.altitude.toString(),
                        gpsPoint.speed.toString()
                )
            }
        }
        return filePath
    }

    /*
    https://github.com/doyaaaaaken/kotlin-csv
     */
    fun writeSensorDataToFile(
            path: String,
            accelerometerData: MutableList<SensorDatapoint>,
            linearAccelerometerData: MutableList<SensorDatapoint>,
            gyroscopeData: MutableList<SensorDatapoint>
    ) : String {
        val filePath = path + "_SENSOR"
        Timber.d("$filePath.csv")

        Timber.d("$accelerometerData")

        val numberOfElements = Math.max(accelerometerData.size, linearAccelerometerData.size, gyroscopeData.size)

        Timber.d("$accelerometerData")

        csvWriter().open("$filePath.csv") {
            writeRow(
                    "Time_in_ns", "ACC_X", "ACC_Y", "ACC_Z",
                    "Time_in_ns", "LINEAR_ACC_X", "LINEAR_ACC_Y", "LINEAR_ACC_Z",
                    "Time_in_ns", "w_X", "w_Y", "w_Z"
            )
            for (i in 0 until numberOfElements) {
                writeRow(
                    convertEvent(accelerometerData.getOrNull(i))
                            + convertEvent(linearAccelerometerData.getOrNull(i))
                            + convertEvent(gyroscopeData.getOrNull(i))
                )
            }
        }
        return filePath
    }

    private fun convertEvent (
            sensorDatapoint: SensorDatapoint?
    ) : List<String> {
        if (sensorDatapoint != null) {
            return listOf(
                    sensorDatapoint.timestamp.toString(),
                    sensorDatapoint.values[0].toString(),
                    sensorDatapoint.values[1].toString(),
                    sensorDatapoint.values[2].toString()
            )
        }
        return emptyList()
    }


    fun uploadFileToFirebase(path: String, uuid: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference

        val file = Uri.fromFile(File("$path.csv"))
        val csvRef = storageRef.child("trips/$uuid/${file.lastPathSegment}")

        Timber.d("trips/$uuid/${file.lastPathSegment}")

        val metadata = storageMetadata {
            contentType = "trip/csv"
        }

        val uploadTask = csvRef.putFile(file, metadata)
        uploadTask.addOnFailureListener {
            Timber.d("Upload not successful")
        }.addOnSuccessListener {
            Timber.d("Upload successful")
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

    fun locationToLatLng(location: Location) : LatLng {
        return LatLng(location.latitude, location.longitude)
    }

    fun convertLabelToJSON(label : Labels) : String {
        val gson = Gson()
        return gson.toJson(label)
    }

    fun retrieveLabelFromJSON(json : String) : Labels {
        val gson = Gson()
        return gson.fromJson(json, Labels::class.java)
    }



}
