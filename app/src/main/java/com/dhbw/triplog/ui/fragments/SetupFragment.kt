package com.dhbw.triplog.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
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

/**
 * SetupFragment, showing a DSGVO Info Text to the user to agree to the collection of user data.
 * With agreeing to the DSGVO, a unique user ID will be created to associate him anonymously with
 * the data being uploaded to the Google Firebase Storage instance
 *
 * @property sharedPref Private key-value storage
 */
@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    @Inject
    lateinit var sharedPref: SharedPreferences

    /**
     * Being called when the View is created. Is forwarding the user to the tripFragment if
     * the DSGVO information was already accepted. Is setting up the Accept/Decline Buttons for the
     * DSGVO agreement.
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDsgvo.text = HtmlCompat.fromHtml(
                getString(R.string.DSGVO),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        if(sharedPref.getBoolean(KEY_DSGVO, false)) {
            val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.setupFragment, true)
                    .build()
            findNavController().navigate(
                    R.id.action_setupFragment_to_TripFragment,
                    null,
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

    /**
     * Is used to store the agreement of the DSGVO in the persistent SharedPreferences Storage
     */
    private fun writeDSGVOAcceptedToSharedPref() {
        sharedPref.edit().putBoolean(KEY_DSGVO, true).apply()
    }

}