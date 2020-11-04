package com.example.triplog

import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import nl.joery.animatedbottombar.AnimatedBottomBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val BACK_STACK_ROOT_TAG = "HomeFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (supportActionBar != null)
            supportActionBar?.hide()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment.newInstance("null", "null"))
            .addToBackStack(BACK_STACK_ROOT_TAG)
            .commit()

        bottom_bar.onTabSelected = {
            val newFragment: Fragment? = when (it.id) {
                R.id.home -> HomeFragment.newInstance("null", "null")
                R.id.settings -> SettingsFragment.newInstance("null", "null")
                R.id.map -> MapFragment.newInstance("null", "null")
                else -> null
            }
            if (newFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, newFragment)
                    .commit()
            }
        }


    }

    private fun initNavBar() {
        bottom_bar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab

            ) {
                Log.d("bottom_bar", "Selected index: $newIndex, title: ${newTab.title}")
            }

            // An optional method that will be fired whenever an already selected tab has been selected again.
            override fun onTabReselected(index: Int, tab: AnimatedBottomBar.Tab) {
                Log.d("bottom_bar", "Reselected index: $index, title: ${tab.title}")
            }
        })
    }

    override fun onBackPressed() {
        bottom_bar.selectTabById(R.id.home)
    }

}