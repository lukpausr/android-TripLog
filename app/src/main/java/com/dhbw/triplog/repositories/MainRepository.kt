package com.dhbw.triplog.repositories

import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.db.TripDao
import javax.inject.Inject

class MainRepository @Inject constructor(
    val tripDao: TripDao
){
    suspend fun insertTrip(trip: Trip) = tripDao.insertTrip(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    fun getAllTripsSortedByDate() = tripDao.getAllTripsSortedByDate()

    fun getAllTripsSortedByTimeInMillis() = tripDao.getAllTripsSortedByTimeInMillis()

}