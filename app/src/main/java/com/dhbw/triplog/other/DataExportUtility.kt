package com.dhbw.triplog.other

import android.location.Location
import android.net.Uri
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import timber.log.Timber
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat

object DataExportUtility {

    fun writeGPSDataToFile(file: File, gpsPoints: MutableList<Location>, selectedTransportType: Labels?) : String {

        val labels = labelsToString(selectedTransportType)

        val filename = file.absolutePath +
                "/" +
                System.currentTimeMillis().toString() + labels

        Timber.d(filename)

        val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd:HH:mm:ss:SS:z")

        csvWriter().open("$filename.csv") {
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

        return filename

    }

    fun uploadFileToFirebase(path: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference

        val file = Uri.fromFile(File(path + ".csv"))
        val csvRef = storageRef.child("trips/${file.lastPathSegment}")

        Timber.d("trips/${file.lastPathSegment}")

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



}