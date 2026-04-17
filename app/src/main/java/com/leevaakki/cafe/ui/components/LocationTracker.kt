package com.leevaakki.cafe.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.leevaakki.cafe.viewmodel.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationTracker(
    private val context: Context,
    private val viewModel: ChatViewModel
) {
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var trackingJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startTracking(orderId: String) {
        trackingJob?.cancel()
        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        viewModel.updateOrderLocation(orderId, it.latitude, it.longitude)
                    }
                }
                delay(10000) // Update every 10 seconds
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }
}
