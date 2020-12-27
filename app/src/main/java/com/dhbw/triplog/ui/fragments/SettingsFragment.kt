package com.dhbw.triplog.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.dhbw.triplog.R
import com.dhbw.triplog.other.DeviceRandomUUID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

/**
 * SettingsFragment, enabling the user to (change) and view settings and user variables
 *
 * @property sharedPref Private key-value storage
 */
@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPref: SharedPreferences

    /**
     * Being called when the View is created. Is setting up the UserID TextView to print out
     * the users unique ID for Debugging purposes with Google Firebase Storage.
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvUserID.text = DeviceRandomUUID.getRUUID(sharedPref)
    }
}