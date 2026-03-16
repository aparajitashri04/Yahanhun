package com.alice.yahanhun.utils

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase

class LocationTracker(private val context: Context, private val userId: String) {

    private val database = FirebaseDatabase.getInstance(
        "https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private val locationRef = database.getReference("userLocation").child(userId)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])
    fun startTracking() {
        val interval = getIntervalBasedOnBattery()

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    val data = mapOf(
                        "latitude" to LocationCrypto.encrypt(it.latitude),
                        "longitude" to LocationCrypto.encrypt(it.longitude),
                        "timestamp" to System.currentTimeMillis()
                    )
                    locationRef.setValue(data)

                    sharedPrefs.edit()
                        .putString("LATITUDE", it.latitude.toString())
                        .putString("LONGITUDE", it.longitude.toString())
                        .apply()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])
    fun setUpdateInterval(intervalMillis: Long) {
        if (!::locationCallback.isInitialized) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun getIntervalBasedOnBattery(): Long {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return when (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) {
            in 0..15 -> {
                Toast.makeText(context, "Battery low! Location updates slowed.", Toast.LENGTH_LONG).show()
                120_000L
            }
            in 16..30 -> 60_000L
            else -> 10_000L
        }
    }
}