package com.dhbw.triplog.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.repositories.MainRepository
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    private val tripsSortedByDate = mainRepository.getAllTripsSortedByDate()

    val trips = MediatorLiveData<List<Trip>>()

    fun insertTrip(trip: Trip) = viewModelScope.launch {
        mainRepository.insertTrip(trip)
    }

    init {
        trips.addSource(tripsSortedByDate) {
            it?.let { trips.value = it }
        }
    }

}