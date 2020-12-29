package com.dhbw.triplog.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.dhbw.triplog.R
import com.dhbw.triplog.other.Constants.ACTION_SHOW_TRIP_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Main Activity which is holding the fragments and setting up the navigation bar
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /**
     * Is called on creation of the activity and is setting up the navigation (bar)
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigate user directly to the Trip Fragment depending on the intent
        // navigateToTripFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)

        // Set up the navigation controller for the bottom navigation view
        bottomNavView.setupWithNavController(navHostFragment.findNavController())
        bottomNavView.setOnSystemUiVisibilityChangeListener {  }

        // Depending on the current navigation destination, the navigation is either shown
        // or not shown
        navHostFragment.findNavController()
                .addOnDestinationChangedListener {_, destination, _ ->
                    when(destination.id) {
                        R.id.settingsFragment, R.id.tripFragment, R.id.infoFragment ->
                            bottomNavView.visibility = View.VISIBLE
                        else -> bottomNavView.visibility = View.GONE
                    }
                }
    }

    /**
     * Defining what happens when a new intent is arriving
     *
     * @param intent
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTripFragmentIfNeeded(intent)
    }

    /**
     * Automatically navigating the user to the TripFragment
     *
     * @param intent
     */
    private fun navigateToTripFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_TRIP_FRAGMENT) {
            navHostFragment.findNavController().navigate(R.id.action_global_tripFragment)
        }
    }
}