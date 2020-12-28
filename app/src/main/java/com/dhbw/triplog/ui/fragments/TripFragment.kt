package com.dhbw.triplog.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
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
import com.dhbw.triplog.adapters.RecyclerViewCallback
import com.dhbw.triplog.adapters.VehicleItem
import com.dhbw.triplog.adapters.VehicleSelectionAdapter
import com.dhbw.triplog.db.Trip
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE
import com.dhbw.triplog.other.Constants.KEY_SELECTED_LABEL
import com.dhbw.triplog.other.Constants.LOCATION_SETTING_REQUEST
import com.dhbw.triplog.other.Constants.MAP_ZOOM
import com.dhbw.triplog.other.Constants.POLYLINE_COLOR
import com.dhbw.triplog.other.Constants.POLYLINE_WIDTH
import com.dhbw.triplog.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.dhbw.triplog.other.DataUtility
import com.dhbw.triplog.other.Labels
import com.dhbw.triplog.other.TrackingUtility
import com.dhbw.triplog.services.TrackingService
import com.dhbw.triplog.ui.viewmodels.MainViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_trip.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject


/**
 * TripFragment, being used for starting and stopping trip record session as well as for vehicle
 * selection to label the different trips before starting them.
 * With this way, labeling can be ensured.
 *
 * @property sharedPref Private key-value storage
 * @property viewModel Connection between Repository and View
 * @property isTracking Information about the current Tracking State, updated by TrackingService
 * @property gpsPoints List of all gpsPoints of the current recording session
 * @property gpsPointsLatLng List of all gpsPoints of the current recording session in LatLng format
 * @property map GoogleMaps Instance
 * @property filterPopup Popup window for vehicle selection
 * @property selectedVehicle Currently selected Vehicle
 */
@AndroidEntryPoint
class TripFragment : Fragment(R.layout.fragment_trip), EasyPermissions.PermissionCallbacks {

    @Inject
    lateinit var sharedPref: SharedPreferences

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var gpsPoints = mutableListOf<Location>()
    private var gpsPointsLatLng = mutableListOf<LatLng>()

    private var map: GoogleMap? = null

    private var filterPopup: PopupWindow? = null
    private var selectedVehicle: Int = -1

    /**
     * Being called when the View is created. Checks if all permissions are granted, subscribes to
     * observers, is setting up click listeners for the different buttons and is creating a
     * Google Maps instance
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)

        requestPermissions()
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
            if(!isTracking && selectedVehicle != -1) {
                enableLocation()
            } else if (selectedVehicle == -1) {
                Toast.makeText(
                        context,
                        "Please select the transportation type first!",
                        Toast.LENGTH_LONG
                ).show()
            }
        }
        btnStopRecord.setOnClickListener {
            if(isTracking) {
                gpsPoints = TrackingService.allGpsPoints
                zoomToWholeTrack()
                saveData()
            }
        }

        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }
    }

    /**
     * Subscribe to data changes in the LiveData Objects of the Tracking Service
     * The current Tracking State is being used to update all UI Elements on a change, while the
     * GPS Points are being used to update the map element
     * The Activity Updates are currently unused but can be used to interact with the Google
     * Activity Recognition API
     */
    private fun subscribeToObservers() {
        TrackingService.gpsPoints.observe(viewLifecycleOwner, Observer {
            gpsPoints.add(it)
            gpsPointsLatLng.add(DataUtility.locationToLatLng(it))
            addLatestPolyline()
            moveCameraToUser()
        })
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            if (isTracking != it) {
                isTracking = it
                refreshTvTrackingState()
                refreshButtonColor()
            }
        })
    }

    /**
     * Refreshes the Tracking State Annotation in the top right corner to display
     * whether the app is currently tracking your trip or not
     */
    private fun refreshTvTrackingState() {
        if(isTracking) {
            tvTrackingState.text = resources.getString(R.string.TRACKING_ACTIVE)
        } else {
            tvTrackingState.text = resources.getString(R.string.TRACKING_INACTIVE)
        }
    }

    /**
     * Refreshes the Color of the "Start / Stop - Recording" Buttons depending on the
     * current tracking state
     */
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

    /**
     * Stop Tracking Service by sending a "Stop" command to the service
     * After stopping the service the map, gpsPoints and Vehicle selection are reset
     */
    private fun stopTracking() {
        sendCommandToService(ACTION_STOP_SERVICE)
        gpsPointsLatLng.clear()     // Clear the gpsPointsLatLng and gpsPoints
        gpsPoints.clear()           // array in preparation for next record
        map?.clear()                // Clear the map output
        selectedVehicle = -1           // Set the currently selected vehicle to "undefined" to
                                    // force the user to select it again before a new record
    }

    /**
     * Start Tracking Service by sending a "Start" command to the Tracking Service
     */
    private fun startTracking() {
        sendCommandToService(ACTION_START_RESUME_SERVICE)
    }

    /**
     * Save the currently selected Label object to SharedPreferences
     * Because you cannot save non-primitive objects in SharedPreferences, the Label object is
     * converted into json to be saved as string
     *
     * @param label Currently selected label
     */
    private fun writeLabelToSharedPref(label: Labels) {
        val json = DataUtility.convertLabelToJSON(label)
        sharedPref.edit().putString(KEY_SELECTED_LABEL, json).apply()
    }

    /**
     * Return the currently selected Label object from SharedPreferences
     * Because it is a json string, we need to convert it back to a Label object
     *
     * @return Currently selected label
     */
    private fun getLabelFromSharedPref() : Labels {
        val json = sharedPref.getString(KEY_SELECTED_LABEL, "")
        return DataUtility.retrieveLabelFromJSON(json!!)
    }

    /**
     * Saving the recorded data to .csv files. Creating a new DB entry with the recorded data and
     * the links of the .csv files
     * Stops the tracking entirely after saving the data in the .csv files and database
     */
    private fun saveData() {

        // Screenshot the current map
        map?.snapshot { bitmap ->

            // Collect important information: Currently selected label, Current time in Millis
            // The information is being used to create a unique path / filename for the
            // collected data
            val label = getLabelFromSharedPref()
            val timestamp = System.currentTimeMillis()
            val path = DataUtility.getPathAndFilename(
                    requireContext(),
                    label,
                    timestamp
            )

            // Debugging: View all recorded GPS Points in console
            Timber.d("GPS_Points: $gpsPoints")

            // Writing the GPS Data to a .csv file and saving the files' path in csvPathGPS
            val csvPathGPS = DataUtility.writeGPSDataToFile(
                    path,
                    gpsPoints
            )
            // Writing the Sensor Data to a .csv file and saving the files' path in csvPathSensor
            val csvPathSensor = DataUtility.writeSensorDataToFile(
                    path,
                    TrackingService.accelerometerData,
                    TrackingService.linearAccelerometerData,
                    TrackingService.gyroscopeData
            )

            // Creating a new Trip Object with: A screenshot of the map (bitmap), the timestamp,
            // the total recording time provided by the Tracking Service, the current Date,
            // the label as well as both .csv paths. The UploadStatus is set to 'false' because
            // the files are not being uploaded yet.
            val trip = Trip(
                    bitmap,
                    timestamp,
                    TrackingService.tripTimeInMillis.value!!,
                    DataUtility.getFormattedDate(timestamp),
                    DataUtility.convertLabelToJSON(label),
                    csvPathGPS,
                    csvPathSensor,
                    false
            )
            viewModel.insertTrip(trip)

            // Calling the stop Tracking function to stop the service and to delete all data
            // regarding the last recorded trip
            stopTracking()
        }
    }

    /**
     * Move the map / camera to the point the user is currently at
     */
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

    /**
     * Zoom map to see the whole track, to be called before making a screenshot of the trip
     */
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

    /**
     * Add all polylines (line between 2 points) to the map. The points are being provided
     * by the TrackingService because the saved points in the activities will get deleted when
     * we leave the activity
     */
    private fun addAllPolylines() {
        gpsPointsLatLng.clear()
        for (gpsPoint in TrackingService.allGpsPoints) {
            gpsPointsLatLng.add(DataUtility.locationToLatLng(gpsPoint))
        }
        for(latlng in gpsPointsLatLng) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(gpsPointsLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    /**
     * Append the line between the two latest GPS Points to the current Polyline. With this it is
     * not necessary to use the addAllPolylines method each time a GPS point is added
     */
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

    /**
     * Sending a user defined command to the Tracking Service
     *
     * @param action The action which is going to be send to the TrackingService as a String
     */
    private fun sendCommandToService(action: String) =
            Intent(requireContext(), TrackingService::class.java).also {
                it.action = action
                requireContext().startService(it)
            }

    /**
     * Force the user to enable his Location service before calling startTracking(). If location
     * Services are enabled tracking is automatically started. If not, the user has to press
     * 'Start Recording' again.
     * This solution is heavily influenced by:
     * For more detailed information see:
     * https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
     * https://stackoverflow.com/a/31816683
     */
    private fun enableLocation() {
        activity?.let {
            val locationRequest = LocationRequest.create()
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)

            val task = LocationServices.getSettingsClient(it)
                    .checkLocationSettings(builder.build())

            task.addOnSuccessListener { response ->
                val states = response.locationSettingsStates
                if (states.isLocationPresent) {
                    startTracking()
                }
            }

            task.addOnFailureListener { e ->
                if(e is ResolvableApiException) {
                    try {
                        it.startIntentSenderForResult(
                                e.resolution.intentSender,
                                LOCATION_SETTING_REQUEST,
                                null,
                                0,
                                0,
                                0,
                                null)
                    } catch (sendEx: IntentSender.SendIntentException) { }
                }
            }
        }
    }

    /**
     * Requesting user permissions for GPS Tracking via EasyPermissions by calling the
     * requestPermissions method.
     * Depending on the used Android Version, different permission requests apply to the user
     * https://github.com/googlesamples/easypermissions
     */
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
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    /**
     * Direct the user to the system settings screen for this app, if he denied permissions with
     * the "Never Ask Again" option
     * https://github.com/googlesamples/easypermissions#required-permissions
     *
     * @param requestCode
     * @param permissions
     */
    override fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, permissions)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    /**
     * Necessary override because of implementation of PermissionCallbacks interface
     * https://github.com/googlesamples/easypermissions#request-permissions
     *
     * @param requestCode
     * @param perms
     */
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {    }

    /**
     * Override of onRequestPermissionResult according to the EasyPermissions Github Usage Guide
     * https://github.com/googlesamples/easypermissions#basic
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    /**
     * Setting up and showing the PopupWindow for the vehicle selection
     *
     * @return Popup window for vehicle selection
     */
    private fun showAlertFilter(): PopupWindow {
        // Set selectedTransportType variable to Labels.WALK, will be reset to another value
        // later. Reason: when condition is not happy, if the variable is not yet initialized
        var selectedTransportType = Labels.WALK
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(
                R.layout.vehicle_selector,
                rootView,
                false
        )

        // Create a reference to the RecyclerView in vehicle_selector.xml
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvVehicleSelection)
        recyclerView.addItemDecoration(
                DividerItemDecoration(
                        recyclerView.context,
                        DividerItemDecoration.VERTICAL
                )
        )

        // Create a reference to the VehicleSelectionAdapter, required for filling the
        // recycler view with content
        val adapter = VehicleSelectionAdapter(requireContext())
        // Insert the different vehicle selection items (Filter Items) into the adapter
        adapter.addVehicle(getVehicleItems())

        recyclerView.adapter = adapter
        adapter.selectedItem(selectedVehicle)

        // Set a onClickListener on the adapter to identify clicks on specific items
        adapter.setOnClick(object : RecyclerViewCallback<VehicleItem> {
            override fun onItemClicked(view: View, position: Int, item: VehicleItem) {
                selectedVehicle = position
                Timber.d("Label: data = ${item.name}")
                when (item.name) {
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

        // return a PopupWindow Object which can be shown
        return PopupWindow(
                view,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Method in which the available vehicle types for the vehicle selection dialogue are defined.
     * Each Item consists of a String type name and vector graphic according to the vehicle type.
     * This is not to be confused with the actual label objects
     *
     * @return List of FilterItems with all available transport types
     */
    private fun getVehicleItems() : List<VehicleItem> {

        val vehicleItemList = mutableListOf<VehicleItem>()
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_directions_walk_24, "Fuß (gehen)"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_directions_run_24, "Fuß (Joggen)"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_pedal_bike_24, "Fahrrad"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_electric_bike_24, "E-Bike"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_electric_scooter_24, "E-Roller"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_directions_car_24, "Auto (Konventionell)"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_electric_car_24, "Auto (Elektrisch)"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_electric_car_24, "Auto (Hybrid)"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_directions_bus_24, "Bus"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_train_24, "Bahn"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_train_24, "S-Bahn"))
        vehicleItemList.add(VehicleItem(R.drawable.ic_baseline_tram_24, "U-Bahn"))

        return vehicleItemList
    }

    /**
     * Method to close the currently open Popup window and reset it / just reset it if it is not
     * open currently, to update all contents and to close it after selecting an item by calling
     * the method in the PopupWindows click listener
     */
    private fun dismissPopup() {
        filterPopup?.let {
            if(it.isShowing){
                it.dismiss()
            }
            filterPopup = null
        }
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