package com.dhbw.triplog.ui.fragments

import android.bluetooth.BluetoothClass
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.dhbw.triplog.R
import com.dhbw.triplog.other.Constants.KEY_DSGVO
import com.dhbw.triplog.other.DeviceRandomUUID
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_setup.*
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(sharedPref.getBoolean(KEY_DSGVO, false)) {
            val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.setupFragment, true)
                    .build()
            findNavController().navigate(
                    R.id.action_setupFragment_to_TripFragment,
                    savedInstanceState,
                    navOptions
            )
        }

        tvAccept.setOnClickListener {
            writeDSGVOAcceptedToSharedPref()
            DeviceRandomUUID.createRUUID(sharedPref)
            findNavController().navigate(R.id.action_setupFragment_to_TripFragment)
        }

        tvDecline.setOnClickListener {
            Snackbar.make(requireView(), "You cannot use this app without accepting the DSGVO", Snackbar.LENGTH_LONG)
                    .setAnchorView(tvAccept)
                    .show()
        }
    }

    private fun writeDSGVOAcceptedToSharedPref() {
        sharedPref.edit().putBoolean(KEY_DSGVO, true).apply()
    }

}