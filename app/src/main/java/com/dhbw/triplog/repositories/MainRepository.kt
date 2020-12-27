package com.dhbw.triplog.repositories

import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.db.TripDao
import javax.inject.Inject

/**
 * MainRepository class
 *
 * @param tripDao
 */
class MainRepository @Inject constructor(
    private val tripDao: TripDao
) {
    suspend fun insertTrip(trip: Trip) = tripDao.insertTrip(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    fun getAllTripsSortedByDate() = tripDao.getAllTripsSortedByDate()

    fun getAllTripsNotUploaded() = tripDao.getAllTripsNotUploaded()

    suspend fun updateTrip(status: Boolean, id: Int) = tripDao.updateTrip(status, id)
}