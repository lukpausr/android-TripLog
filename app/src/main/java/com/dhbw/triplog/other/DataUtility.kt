package com.dhbw.triplog.other

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Static class like implementation of methods regarding everything to do with the GPS and Sensor
 * Data
 */
object DataUtility {

    /**
     * Creates a unique Path and Filename with a timestamp and the label (vehicle type)
     * the data was recorded with
     *
     * @param context The current context to get the internal storage file path
     * @param selectedTransportType The currently selected vehicle of type Label
     * @param timestamp A timestamp defined at method call, preferably the timestamp the files are
     * created at or this method is being called
     *
     * @return Path and filename
     */
    fun getPathAndFilename(context: Context, selectedTransportType: Labels?, timestamp: Long) : String {
        val path = context.filesDir.toString()
        val labels = labelsToString(selectedTransportType)
        return "$path/$timestamp$labels"
    }

    /**
     * Creates a date string of format 'dd.MM.yyyy HH:mm:ss' from timestamp
     *
     * @param timestamp Timestamp with current time in ms
     *
     * @return String containing the date in format 'dd.MM.yyyy HH:mm:ss'
     */
    fun getFormattedDate(timestamp: Long) : String {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(timestamp)
    }

    /**
     * Converter method, converting a SensorDatapoint to a List containing a timestamp and the
     * sensors x, y and z values
     *
     * @param sensorDatapoint Object containing all information for a single data point
     *
     * @return List containing a timestamp and the sensors for the given data point
     */
    fun convertEvent (
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

    /**
     * Uploads a file at the given path to the Google Firebase Storage instance in a folder
     * defined by the unique user ID
     * For detailed information see:
     * https://firebase.google.com/docs/storage/android/upload-files#upload_from_a_local_file
     *
     * @param path Location of the file to upload
     * @param uuid Unique User ID to specify the folder in which the file is being uploaded to
     */
    fun uploadFileToFirebase(path: String, uuid: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference

        // Create URI from given path
        val file = Uri.fromFile(File(path))
        // Create reference to destination path in Firebase Storage
        val csvRef = storageRef.child("trips/$uuid/${file.lastPathSegment}")

        Timber.d("trips/$uuid/${file.lastPathSegment}")

        val metadata = storageMetadata {
            contentType = "trip/csv"
        }

        // Upload file to destination path in Firebase Storage
        val uploadTask = csvRef.putFile(file, metadata)
        uploadTask.addOnFailureListener {
            Timber.d("Upload not successful")
        }.addOnSuccessListener {
            Timber.d("Upload successful")
            val fileToDelete = File(path)
            fileToDelete.delete()
        }
    }

    /**
     * Used to create a String representation of objects of type Label with all sublabels included.
     * This method is being used to append labels to the filename to be able to identify the
     * used transport type later on.
     *
     * @param selectedTransportType Transport type which had been used while recording the data /
     * file this String is being appended to
     *
     * @return String type representation of all labels of the given transport type
     */
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

    /**
     * Converting Location type objects to LatLng type objects
     *
     * @param location Location type object
     *
     * @return LatLng type object
     */
    fun locationToLatLng(location: Location) : LatLng {
        return LatLng(location.latitude, location.longitude)
    }

    /**
     * Converting label objects to a json String representation to be able to save objects
     * in shared Preferences
     *
     * @param label Label type object (current transport vehicle selection)
     *
     * @return json String type representation of label object
     */
    fun convertLabelToJSON(label : Labels) : String {
        val gson = Gson()
        return gson.toJson(label)
    }

    /**
     * Converting json String type label objects back to label objects to be able to retrieve
     * label objects saved in shared Preferences
     *
     * @param json json String type representation of a label object
     *
     * @return Label type object
     */
    fun retrieveLabelFromJSON(json : String) : Labels {
        val gson = Gson()
        return gson.fromJson(json, Labels::class.java)
    }
}
