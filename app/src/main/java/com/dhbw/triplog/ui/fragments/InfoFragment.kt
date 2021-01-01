package com.dhbw.triplog.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.dhbw.triplog.R
import com.dhbw.triplog.other.Constants.KEY_DSGVO
import com.dhbw.triplog.other.DeviceRandomUUID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_info.*
import javax.inject.Inject

/**
 * InfoFragment, enabling the user to (change) and view settings and user variables
 *
 * @property sharedPref Private key-value storage
 */
@AndroidEntryPoint
class InfoFragment : Fragment(R.layout.fragment_info) {

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
        tvDSGVO.text = getDSGVOStatus()
    }

    /**
     * Returning the DSGVO Status, which is saved in shared Preferences as human readable String
     */
    private fun getDSGVOStatus() : String {
        return if(sharedPref.getBoolean(KEY_DSGVO, false)) {
            "Accepted"
        } else {
            "Denied"
        }
    }
}