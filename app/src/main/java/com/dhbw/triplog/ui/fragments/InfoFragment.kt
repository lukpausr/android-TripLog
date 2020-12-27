package com.dhbw.triplog.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhbw.triplog.R
import com.dhbw.triplog.adapters.TripAdapter
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.other.DataUtility
import com.dhbw.triplog.other.DeviceRandomUUID
import com.dhbw.triplog.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*
import timber.log.Timber
import javax.inject.Inject

/**
 * InfoFragment, enabling the user to view his trips and upload them
 *
 * This class is being used for enabling the user to upload his trips to the Google Firebase
 * Storage and to see his recent trips
 *
 * @property viewModel
 * @property tripAdapter
 * @property sharedPref Private key-value storage
 * @property tripsNotUploaded List which will contain all not yet uploaded trips according to a
 * DB query
 */
@AndroidEntryPoint
class InfoFragment : Fragment(R.layout.fragment_info) {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var tripAdapter: TripAdapter

    @Inject
    lateinit var sharedPref: SharedPreferences

    private var tripsNotUploaded: List<Trip> = emptyList()

    /**
     * Being called when the View is created. Is setting up the RecyclerView as well as a click
     * Listener for the Upload button. Observes DB Data.
     * @param view
     * @param savedInstanceState
     */
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
            val online = isOnline(requireContext())
            if(online) {
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

    /**
     * Being called by "onViewCreated". Sets up the RecyclerView to show all trips currently
     * saved in the DB in the rvTrips UI Element
     */
    private fun setupRecyclerView() = rvTrips.apply {
        tripAdapter = TripAdapter()
        adapter = tripAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Being called when the button btnUpload is being pressed. Uploads all trips (Sensor + GPS
     * Data) to the Google Firebase Storage container
     */
    private fun uploadAllTrips() {
        // Get the Random Unique User ID out of sharedPreferences
        val ruuid = DeviceRandomUUID.getRUUID(sharedPref)
        val trips = mutableListOf<Trip>()
        for(trip in tripsNotUploaded) {
            trips.add(trip)
        }
        // Upload all Data to the Cloud, annotated with the User ID for encrypted
        // User Identification
        for(trip in trips) {
            Timber.d("GPS: ${trip.fileNameGPS} + SENSOR: ${trip.fileNameSensor}")
            trip.fileNameGPS?.let { DataUtility.uploadFileToFirebase(it, ruuid) }
            trip.fileNameSensor?.let { DataUtility.uploadFileToFirebase(it, ruuid) }
            trip.id?.let { viewModel.updateTrip(true, it) }
        }
    }

    /**
     * Check if the Device is online to prevent uploads without internet connection
     * This function is an adapted and modified Stackoverflow solution:
     * https://stackoverflow.com/a/57237708
     * https://stackoverflow.com/a/43444380
     * @param context the current Context (Context, the fragment is associated with)
     * @return Returns true if there is internet connection, false if not
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