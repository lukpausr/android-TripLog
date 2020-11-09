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

    @Query("SELECT * FROM trip_table ORDER BY timeInMillis DESC")
    fun getAllTripsSortedByTimeInMillis(): LiveData<List<Trip>>

}