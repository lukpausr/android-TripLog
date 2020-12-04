package com.dhbw.triplog.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Camera
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.dhbw.triplog.R
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.KEY_TRACKING_STATE
import com.dhbw.triplog.other.Constants.MAP_ZOOM
import com.dhbw.triplog.other.Constants.POLYLINE_COLOR
import com.dhbw.triplog.other.Constants.POLYLINE_WIDTH
import com.dhbw.triplog.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.dhbw.triplog.other.TrackingUtility
import com.dhbw.triplog.services.TrackingService
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
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

    private var gpsPoints = mutableListOf<Location>()
    private var gpsPointsLatLng = mutableListOf<LatLng>()

    private var map: GoogleMap? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.onCreate(savedInstanceState)

        requestPermissions()
        getToggleStateFromSharedPref()

        subscribeToObservers()

        btnStartRecord.setOnClickListener {
            if(!isTracking) {
                isTracking = true
                startTracking()
                writeToggleStateToSharedPref(true)
                refreshButtonColor()
                refreshTvTrackingState()
            }
        }

        btnStopRecord.setOnClickListener {
            if(isTracking) {
                isTracking = false
                stopTracking()
                writeToggleStateToSharedPref(false)
                refreshButtonColor()
                refreshTvTrackingState()
            }
        }

        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

    }

    private fun refreshTvTrackingState() {
        if(isTracking) {
            tvTrackingState.text = "Tracking active"
        } else {
            tvTrackingState.text = "Tracking inactive"
        }
    }

    private fun refreshButtonColor() {
        if(isTracking) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity?.resources?.let { it ->
                    btnStartRecord.setBackgroundColor(
                        it.getColor(
                            R.color.dhbw_grey,
                            requireActivity().theme
                        )
                    )
                }
                activity?.resources?.let { it ->
                    btnStopRecord.setBackgroundColor(
                        it.getColor(
                            R.color.dhbw_red,
                            requireActivity().theme
                        )
                    )
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity?.resources?.let { it ->
                    btnStartRecord.setBackgroundColor(
                        it.getColor(
                            R.color.dhbw_red,
                            requireActivity().theme
                        )
                    )
                }
                activity?.resources?.let { it ->
                    btnStopRecord.setBackgroundColor(
                        it.getColor(
                            R.color.dhbw_grey,
                            requireActivity().theme
                        )
                    )
                }
            }
        }
    }

    private fun getToggleStateFromSharedPref() {
        val state = sharedPref.getBoolean(KEY_TRACKING_STATE, true )
        refreshButtonColor()
        refreshTvTrackingState()
    }

    private fun stopTracking() {
        sendCommandToService(ACTION_STOP_SERVICE)
        gpsPointsLatLng.clear()
        gpsPoints.clear()
        map?.clear()
    }

    private fun startTracking() {
        sendCommandToService(ACTION_START_RESUME_SERVICE)
    }

    private fun writeToggleStateToSharedPref(isChecked : Boolean) {
        sharedPref.edit().putBoolean(KEY_TRACKING_STATE, isChecked).apply()
    }

    private fun subscribeToObservers() {
        TrackingService.gpsPoints.observe(viewLifecycleOwner, Observer {
            gpsPoints.add(it)
            gpsPointsLatLng.add(LocationToLatLng(it))
            addLatestPolyline()
            moveCameraToUser()
        })
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            isTracking = it
        })
        TrackingService.activityUpdates.observe(viewLifecycleOwner, Observer {
        })
    }

    private fun moveCameraToUser() {
        if(gpsPoints.isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LocationToLatLng(gpsPoints.last()),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for(point in gpsPoints) {
            bounds.include(LocationToLatLng(point))
        }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun LocationToLatLng(location: Location) : LatLng {
        return LatLng(location.latitude, location.longitude)
    }

    private fun addAllPolylines() {
        for(latlng in gpsPointsLatLng) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(gpsPointsLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if(gpsPoints.isNotEmpty() && gpsPoints.size > 1) {
            val previousLocation = gpsPoints[gpsPoints.size - 2]
            val currentLocation = gpsPoints.last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(LocationToLatLng(previousLocation))
                .add(LocationToLatLng(currentLocation))
            map?.addPolyline(polylineOptions)
        }
    }


    private fun sendCommandToService(action: String) =
            Intent(requireContext(), TrackingService::class.java).also {
                it.action = action
                requireContext().startService(it)
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

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }


}