package com.dhbw.triplog.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.repositories.MainRepository
import kotlinx.coroutines.launch

/**
 * MainViewModel class, according to the Model-view-viewmodel (MVVM) architectural pattern,
 * this viewModel is being used to expose the data of the ROOM Databse to the apps different
 * views (fragments)
 *
 * @param mainRepository MainRepository for direct interaction with the Room Database
 *
 * @property tripsSortedByDate tripsSortedByDate, provided by mainRepository
 * @property tripsNotUploaded tripsNotUploaded, provided by mainRepository
 * @property tripsToUpload tripsToUpload, reacting to OnChanged events of tripsNotUploaded
 * @property trips trips, reacting to OnChanged events of tripsSortedByDate
 */
class MainViewModel @ViewModelInject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {

    private val tripsSortedByDate = mainRepository.getAllTripsSortedByDate()
    private val tripsNotUploaded = mainRepository.getAllTripsNotUploaded()

    // MediatorLiveData, used to Observe LiveData
    val tripsToUpload = MediatorLiveData<List<Trip>>()
    val trips = MediatorLiveData<List<Trip>>()

    /**
     * Asynchronous operation to insert a new trip into the Room Database
     *
     * @param trip Trip which is going to be inserted into the Database
     */
    fun insertTrip(trip: Trip) = viewModelScope.launch {
        mainRepository.insertTrip(trip)
    }

    /**
     * Asynchronous operation to update the uploadStatus of a trip by id in the Room Database
     *
     * @param status Upload Status
     * @param id Database Entry ID
     */
    fun updateTrip(status: Boolean, id: Int) = viewModelScope.launch {
        mainRepository.updateTrip(status, id)
    }

    /**
     * Initializer block, applying a source to the MediatorLiveData
     */
    init {
        trips.addSource(tripsSortedByDate) {
            it?.let { trips.value = it }
        }
        tripsToUpload.addSource(tripsNotUploaded) {
            it?.let { tripsToUpload.value = it }
        }
    }

}