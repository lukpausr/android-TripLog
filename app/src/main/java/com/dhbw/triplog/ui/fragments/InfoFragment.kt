package com.dhbw.triplog.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhbw.triplog.R
import com.dhbw.triplog.adapters.TripAdapter
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.other.DataUtility
import com.dhbw.triplog.other.DeviceRandomUUID
import com.dhbw.triplog.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class InfoFragment : Fragment(R.layout.fragment_info) {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var tripAdapter: TripAdapter

    @Inject
    lateinit var sharedPref: SharedPreferences

    private var tripsNotUploaded: List<Trip> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        viewModel.trips.observe(viewLifecycleOwner, Observer {
            tripAdapter.submitList(it)
        })
        viewModel.tripsToUpload.observe(viewLifecycleOwner, Observer {
            tripsNotUploaded = it
        })

        btnUpload.setOnClickListener {
            uploadAllTrips()
        }

    }

    private fun setupRecyclerView() = rvTrips.apply {
        tripAdapter = TripAdapter()
        adapter = tripAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun uploadAllTrips() {
        val ruuid = DeviceRandomUUID.getRUUID(sharedPref)
        for(trip in tripsNotUploaded) {
            Timber.d("GPS: ${trip.fileNameGPS} + SENSOR: ${trip.fileNameSensor}")
            trip.fileNameGPS?.let { DataUtility.uploadFileToFirebase(it, ruuid) }
            trip.fileNameSensor?.let { DataUtility.uploadFileToFirebase(it, ruuid) }

            trip.id?.let { viewModel.updateTrip(true, it) }
        }
    }

}