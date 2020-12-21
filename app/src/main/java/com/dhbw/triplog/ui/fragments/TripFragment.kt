package com.dhbw.triplog.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.dhbw.triplog.R
import com.dhbw.triplog.adapters.AlertFilterAdapter
import com.dhbw.triplog.adapters.RecyclerviewCallbacks
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.other.*
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.KEY_SELECTED_LABEL
import com.dhbw.triplog.other.Constants.KEY_TRACKING_STATE
import com.dhbw.triplog.other.Constants.MAP_ZOOM
import com.dhbw.triplog.other.Constants.POLYLINE_COLOR
import com.dhbw.triplog.other.Constants.POLYLINE_WIDTH
import com.dhbw.triplog.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.dhbw.triplog.services.TrackingService
import com.dhbw.triplog.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_trip.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TripFragment : Fragment(R.layout.fragment_trip), EasyPermissions.PermissionCallbacks {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var gpsPoints = mutableListOf<Location>()
    private var gpsPointsLatLng = mutableListOf<LatLng>()

    private var map: GoogleMap? = null

    @Inject
    lateinit var sharedPref: SharedPreferences

    private var filterPopup: PopupWindow? = null
    private var selectedItem: Int = -1

    private var storage: FirebaseStorage = Firebase.storage

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView.onCreate(savedInstanceState)

        requestPermissions()
        getToggleStateFromSharedPref()

        subscribeToObservers()

        btnVehicleSelection.setOnClickListener {
            dismissPopup()
            filterPopup = showAlertFilter()
            filterPopup?.isOutsideTouchable = true
            filterPopup?.isFocusable = true
            filterPopup?.showAsDropDown(
                btnVehicleSelection,
                0,
                0,
                Gravity.BOTTOM
            )
        }

        btnStartRecord.setOnClickListener {
            if(!isTracking && selectedItem != -1) {
                isTracking = true
                startTracking()
                writeToggleStateToSharedPref(true)
                refreshButtonColor()
                refreshTvTrackingState()
            } else if (selectedItem == -1) {
                Toast.makeText(
                    context,
                    "Please select the transportation type first!",
                    Toast.LENGTH_LONG
                ).show()
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
        val state = sharedPref.getBoolean(KEY_TRACKING_STATE, true)
        refreshButtonColor()
        refreshTvTrackingState()
    }

    private fun stopTracking() {

        gpsPoints = TrackingService.allGpsPoints

        zoomToWholeTrack()
        saveData()

        sendCommandToService(ACTION_STOP_SERVICE)

        gpsPointsLatLng.clear()
        gpsPoints.clear()
        map?.clear()
        selectedItem = -1

    }

    private fun startTracking() {
        sendCommandToService(ACTION_START_RESUME_SERVICE)
    }

    private fun writeToggleStateToSharedPref(isChecked: Boolean) {
        sharedPref.edit().putBoolean(KEY_TRACKING_STATE, isChecked).apply()
    }

    private fun writeLabelToSharedPref(label : Labels) {
        val json = DataUtility.convertLabelToJSON(label)
        sharedPref.edit().putString(KEY_SELECTED_LABEL, json).apply()
    }

    private fun getLabelFromSharedPref() : Labels {
        val json = sharedPref.getString(KEY_SELECTED_LABEL, "")
        return DataUtility.retrieveLabelFromJSON(json!!)
    }

    private fun subscribeToObservers() {
        TrackingService.gpsPoints.observe(viewLifecycleOwner, Observer {
            gpsPoints.add(it)
            gpsPointsLatLng.add(DataUtility.locationToLatLng(it))
            addLatestPolyline()
            moveCameraToUser()
        })
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            isTracking = it
        })
        TrackingService.activityUpdates.observe(viewLifecycleOwner, Observer {
        })
    }

    private fun saveData() {
        map?.snapshot { bitmap ->

            val label = getLabelFromSharedPref()
            val timestamp = System.currentTimeMillis()
            val path = DataUtility.getPathAndFilename(
                requireContext(),
                label,
                timestamp
            )

            val csvPath = DataUtility.writeGPSDataToFile(path, gpsPoints)
            DataUtility.uploadFileToFirebase(csvPath)

            val trip = Trip(
                bitmap,
                timestamp,
                TrackingService.tripTimeInMillis.value!!,
                DataUtility.getFormattedDate(timestamp),
                DataUtility.convertLabelToJSON(label),
                csvPath,
                true
            )

            viewModel.insertTrip(trip)

        }
    }

    private fun moveCameraToUser() {
        if(gpsPoints.isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    DataUtility.locationToLatLng(gpsPoints.last()),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for(point in gpsPoints) {
            bounds.include(DataUtility.locationToLatLng(point))
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
                .add(DataUtility.locationToLatLng(previousLocation))
                .add(DataUtility.locationToLatLng(currentLocation))
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

    private fun showAlertFilter(): PopupWindow {
        var selectedTransportType = Labels.WALK
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(
            R.layout.vehicle_selector,
            rootView,
            false
        )
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )

        val adapter = AlertFilterAdapter(requireContext())
        adapter.addAlertFilter(getFilterItems())

        recyclerView.adapter = adapter
        adapter.selectedItem(selectedItem)

        adapter.setOnClick(object : RecyclerviewCallbacks<FilterItem> {
            override fun onItemClick(view: View, position: Int, item: FilterItem) {
                selectedItem = position
                Timber.d("Label: data = ${item.name.toString()}")
                when(item.name.toString()) {
                    "Fuß (gehen)" -> selectedTransportType = Labels.WALK
                    "Fuß (Joggen)" -> selectedTransportType = Labels.RUN
                    "Fahrrad" -> selectedTransportType = Labels.BIKE
                    "E-Bike" -> selectedTransportType = Labels.E_BIKE
                    "E-Roller" -> selectedTransportType = Labels.E_SCOOTER
                    "Auto (Konventionell)" -> selectedTransportType = Labels.CAR
                    "Auto (Elektrisch)" -> selectedTransportType = Labels.ELECTRIC_CAR
                    "Auto (Hybrid)" -> selectedTransportType = Labels.HYBRID_CAR
                    "Bus" -> selectedTransportType = Labels.BUS
                    "Bahn" -> selectedTransportType = Labels.TRAIN
                    "S-Bahn" -> selectedTransportType = Labels.S_TRAIN
                    "U-Bahn" -> selectedTransportType = Labels.SUBWAY
                }
                writeLabelToSharedPref(selectedTransportType)
                dismissPopup()
            }
        })

        return PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    }

    private fun getFilterItems() : List<FilterItem> {

        val filterItemList = mutableListOf<FilterItem>()
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_walk_24, "Fuß (gehen)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_run_24, "Fuß (Joggen)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_pedal_bike_24, "Fahrrad"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_bike_24, "E-Bike"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_scooter_24, "E-Roller"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_car_24, "Auto (Konventionell)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_car_24, "Auto (Elektrisch)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_car_24, "Auto (Hybrid)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_bus_24, "Bus"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_train_24, "Bahn"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_train_24, "S-Bahn"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_tram_24, "U-Bahn"))

        return filterItemList
    }

    private fun dismissPopup() {
        filterPopup?.let {
            if(it.isShowing){
                it.dismiss()
            }
            filterPopup = null
        }

    }


}