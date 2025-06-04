package com.alice.yahanhun

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alice.yahahun.R
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HomeActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load osmdroid configuration
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        setContentView(R.layout.layout_home_activity)

        // Initialize the map
        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        // Center map to Bangalore
        val bangalore = GeoPoint(12.9716, 77.5946)
        map.controller.setZoom(7.0)  // Zoomed out a bit to show multiple cities
        map.controller.setCenter(bangalore)

        addMarker(GeoPoint(12.9716, 77.5946), "Bangalore", R.drawable.ic_marker_red_background)
        addMarker(GeoPoint(13.0827, 80.2707), "Chennai", R.drawable.ic_marker_blue_foreground)
        addMarker(GeoPoint(17.3850, 78.4867), "Hyderabad", R.drawable.ic_marker_green_foreground)
        addMarker(GeoPoint(19.0760, 72.8777), "Mumbai", R.drawable.ic_marker_yellow_foreground)
    }

    private fun addMarker(location: GeoPoint, title: String, iconResId: Int) {
        val marker = Marker(map)
        marker.position = location
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val drawable = ContextCompat.getDrawable(this, iconResId)
        drawable?.let {
            marker.icon = it
        }

        map.overlays.add(marker)
    }
}


