package com.dhbw.triplog.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM trip_table ORDER BY timestamp DESC")
    fun getAllTripsSortedByDate(): LiveData<List<Trip>>

    @Query("SELECT * FROM trip_table WHERE uploadStatus = 0")
    fun getAllTripsNotUploaded(): LiveData<List<Trip>>

    @Query("UPDATE trip_table SET uploadStatus = :status WHERE id =:id")
    suspend fun updateTrip(status: Boolean, id: Int)

}