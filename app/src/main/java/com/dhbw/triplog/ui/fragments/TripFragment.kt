package com.dhbw.triplog.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.dhbw.triplog.R
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.KEY_TRACKING_STATE
import com.dhbw.triplog.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.dhbw.triplog.other.TrackingUtility
import com.dhbw.triplog.services.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_trip.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

@AndroidEntryPoint
class TripFragment : Fragment(R.layout.fragment_trip), EasyPermissions.PermissionCallbacks {

    private var isTracking = false

    @Inject
    lateinit var sharedPref: SharedPreferences


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissions()
        getToggleStateFromSharedPref()

        subscribeToObservers()

        swToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startTracking()
            } else {
                stopTracking()
            }
            writeToggleStateToSharedPref(isChecked)
        }

    }

    private fun getToggleStateFromSharedPref() {
        val state = sharedPref.getBoolean(KEY_TRACKING_STATE, true )
        swToggleButton.isChecked = state
        if (state) {
            startTracking()
        } else {
            stopTracking()
        }
        updateTVToggleButton()
    }

    private fun stopTracking() {
        if(isTracking) sendCommandToService(ACTION_STOP_SERVICE)
    }

    private fun startTracking() {
        if(!isTracking) sendCommandToService(ACTION_START_RESUME_SERVICE)
    }

    private fun updateTVToggleButton() {
        if(isTracking) {
            tvToggleButton.text = "Tracking enabled"
        } else {
            tvToggleButton.text = "Tracking stopped"
        }
    }

    private fun writeToggleStateToSharedPref(isChecked : Boolean) {
        sharedPref.edit().putBoolean(KEY_TRACKING_STATE, isChecked).apply()
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            //tvTripExplain.text = it.toString()
            isTracking = it
            updateTVToggleButton()
        })
        TrackingService.activityUpdates.observe(viewLifecycleOwner, Observer {
            tvTripExplain.text = it
        })
    }

    private fun sendCommandToService(action: String) =
            Intent(requireContext(), TrackingService::class.java).also {
                it.action = action
                requireContext().startService(it)
            }

    private fun setupRecyclerView() = rvTrips.apply {
        TODO("Not yet implemented")
    }

    private fun requestPermissions() {
        if(TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    REQUEST_CODE_LOCATION_PERMISSION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "You need to accept location permissions to use this app.",
                    REQUEST_CODE_LOCATION_PERMISSION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, permissions)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}