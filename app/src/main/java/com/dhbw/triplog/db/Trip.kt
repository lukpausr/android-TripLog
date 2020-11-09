package com.dhbw.triplog.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_table")
data class Trip(
    var img: Bitmap? = null,
    var timestamp: Long = 0L,
    var timeInMillis: Long = 0L
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}