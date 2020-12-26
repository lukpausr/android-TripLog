package com.dhbw.triplog.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
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
            if(isOnline(requireContext())) {
                uploadAllTrips()
            } else {
                Toast.makeText(
                        context,
                        "You do not have internet connection, please try again later",
                        Toast.LENGTH_SHORT
                ).show()
            }
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

    /*
    Adapted and modified:
    https://stackoverflow.com/a/57237708
    https://stackoverflow.com/a/43444380
     */
    private fun isOnline(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if(capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> return true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> return true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
                }
            }
            return false
        } else {
            val netWorkInfo = connectivityManager.allNetworks
            netWorkInfo.forEach {
                val info = connectivityManager.getNetworkInfo(it)
                if(info!!.state == NetworkInfo.State.CONNECTED) return true
            }
            return false
        }
    }
}