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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateToTripFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)


        bottomNavView.setupWithNavController(navHostFragment.findNavController())
        bottomNavView.setOnSystemUiVisibilityChangeListener {  }

        navHostFragment.findNavController()
                .addOnDestinationChangedListener {_, destination, _ ->
                    when(destination.id) {
                        R.id.settingsFragment, R.id.tripFragment, R.id.infoFragment ->
                            bottomNavView.visibility = View.VISIBLE
                        else -> bottomNavView.visibility = View.GONE
                    }
                }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTripFragmentIfNeeded(intent)
    }

    private fun navigateToTripFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_TRIP_FRAGMENT) {
            navHostFragment.findNavController().navigate(R.id.action_global_tripFragment)
        }
    }

}