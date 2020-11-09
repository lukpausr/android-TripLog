package com.dhbw.triplog.services

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.dhbw.triplog.other.Constants.ACTION_PAUSE_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_START_RESUME_SERVICE
import com.dhbw.triplog.other.Constants.ACTION_STOP_SERVICE

class TrackingService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_RESUME_SERVICE -> {
                    Log.d("SERVICE", "Started / Resumed Service")
                }
                ACTION_PAUSE_SERVICE -> {
                    Log.d("SERVICE", "Paused Service")
                }
                ACTION_STOP_SERVICE -> {
                    Log.d("SERVICE", "Stopped Service")
                }
                else -> Log.d("ERROR", "ERROR")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

}