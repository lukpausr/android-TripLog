package com.dhbw.triplog.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dhbw.triplog.other.Labels

@Entity(tableName = "trip_table")
data class Trip(
    var img: Bitmap? = null,
    var timestamp: Long = 0L,
    var timeInMillis: Long = 0L,

    var date: String? = null,
    var label: String? = null,
    var fileName: String? = null,
    var uploadStatus: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}