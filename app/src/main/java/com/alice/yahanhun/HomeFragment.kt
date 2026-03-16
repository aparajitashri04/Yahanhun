package com.alice.yahanhun.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.alice.yahanhun.ui.theme.YahanHunTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var userId: String
    private val database = FirebaseDatabase.getInstance(
        "https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private var googleMap: GoogleMap? = null
    private var currentUserMarker: Marker? = null
    private val friendMarkers = mutableMapOf<String, Marker>()
    private var isInitialZoom = true

    private var userLocationListener: ValueEventListener? = null
    private var friendsListener: ValueEventListener? = null
    private val friendLocationListeners = mutableMapOf<String, ValueEventListener>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPrefs = requireContext()
            .getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userId = sharedPrefs.getString("USER_ID", null) ?: ""

        return ComposeView(requireContext()).apply {
            setContent {
                YahanHunTheme {
                    HomeMapScreen(
                        onMapViewCreated = { fragmentContainerView ->
                            setupMap(fragmentContainerView)
                        },
                        onZoomIn = { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) },
                        onZoomOut = { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }
                    )
                }
            }
        }
    }

    private fun setupMap(containerView: FragmentContainerView) {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(containerView.id, mapFragment)
            .commit()

        mapFragment.getMapAsync { map ->
            googleMap = map

            // Disable built-in controls since we use custom ones
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = true
            map.uiSettings.isCompassEnabled = true

            val defaultLocation = LatLng(20.2961, 85.8245)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

            if (userId.isNotEmpty()) {
                showCurrentUserMarker()
                listenToFriendLocations()
            }
        }
    }

    private fun getCoordinate(snapshot: DataSnapshot, key: String): Double? {
        val raw = snapshot.child(key).value ?: return null
        return when (raw) {
            is Double -> raw
            is Long -> raw.toDouble()
            is String -> {
                try {
                    com.alice.yahanhun.utils.LocationCrypto.decrypt(raw)
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "Decrypt failed for $key: ${e.message}")
                    null
                }
            }
            else -> null
        }
    }

    private fun showCurrentUserMarker() {
        val ref = database.getReference("userLocation").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = getCoordinate(snapshot, "latitude") ?: return
                val lon = getCoordinate(snapshot, "longitude") ?: return
                val map = googleMap ?: return

                val location = LatLng(lat, lon)

                if (isInitialZoom) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
                    isInitialZoom = false
                }

                if (currentUserMarker == null) {
                    currentUserMarker = map.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("You")
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_ORANGE
                                )
                            )
                    )
                } else {
                    currentUserMarker?.position = location
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userLocationListener = listener
        ref.addValueEventListener(listener)
    }

    private fun listenToFriendLocations() {
        val ref = database.getReference("friends").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (friendSnap in snapshot.children) {
                    val friendId = friendSnap.key ?: continue
                    if (!friendLocationListeners.containsKey(friendId)) {
                        showFriendMarker(friendId)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        friendsListener = listener
        ref.addValueEventListener(listener)
    }

    private fun showFriendMarker(friendId: String) {
        val ref = database.getReference("userLocation").child(friendId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = getCoordinate(snapshot, "latitude") ?: return
                val lon = getCoordinate(snapshot, "longitude") ?: return
                val map = googleMap ?: return

                val location = LatLng(lat, lon)

                if (friendMarkers[friendId] == null) {
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("Friend")
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_AZURE
                                )
                            )
                    )
                    if (marker != null) friendMarkers[friendId] = marker
                } else {
                    friendMarkers[friendId]?.position = location
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        friendLocationListeners[friendId] = listener
        ref.addValueEventListener(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        userLocationListener?.let {
            database.getReference("userLocation").child(userId).removeEventListener(it)
        }
        friendsListener?.let {
            database.getReference("friends").child(userId).removeEventListener(it)
        }
        friendLocationListeners.forEach { (friendId, listener) ->
            database.getReference("userLocation").child(friendId).removeEventListener(listener)
        }
        friendLocationListeners.clear()
        friendMarkers.clear()
        googleMap = null
    }
}

@Composable
fun HomeMapScreen(
    onMapViewCreated: (FragmentContainerView) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    val containerId = remember { View.generateViewId() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = containerId
                    onMapViewCreated(this)
                }
            }
        )

        // Custom zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom in
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1F22),
                shadowElevation = 4.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onZoomIn) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = Color(0xFFFB8500),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom out
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1F22),
                shadowElevation = 4.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onZoomOut) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = Color(0xFFFB8500),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}