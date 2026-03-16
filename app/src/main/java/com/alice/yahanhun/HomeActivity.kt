package com.alice.yahanhun

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import com.alice.yahanhun.fragments.*
import com.alice.yahanhun.ui.theme.*
import com.alice.yahanhun.utils.LocationTracker
import com.alice.yahahun.R
import com.alice.yahanhun.utils.NetworkUtils
import org.osmdroid.config.Configuration
import com.alice.yahanhun.utils.PowerButtonReceiver
import com.google.firebase.database.FirebaseDatabase

class HomeActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val SOS_PERMISSIONS_REQUEST_CODE = 2002
    private val REQUIRED_SOS_PERMISSIONS = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.SEND_SMS
    )

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var locationTracker: LocationTracker
    private lateinit var fragmentManager: FragmentManager
    private lateinit var powerButtonReceiver: PowerButtonReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        fragmentManager = supportFragmentManager

        checkAllPermissions()

        // Check if user should see SOS button
        val showSOS = shouldShowSOS()

        setContent {
            YahanHunTheme {
                HomeScreen(
                    showSOSButton = showSOS,
                    onSOSClick = { triggerSOS() },
                    fragmentManager = fragmentManager,
                    sharedPrefs = sharedPrefs
                )
            }
        }
        powerButtonReceiver = PowerButtonReceiver {
            // Run on main thread since BroadcastReceiver can fire off main
            runOnUiThread { triggerSOS() }
        }
        // Handle deep link
        intent?.data?.getQueryParameter("userId")?.let { sharedUserId ->
            val fragment = HomeFragment().apply {
                arguments = Bundle().apply {
                    putString("SHARED_USER_ID", sharedUserId)
                }
            }
            loadFragment(fragment)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStart() {
        super.onStart()
        val userId = sharedPrefs.getString("USER_ID", null) ?: return
        locationTracker = LocationTracker(this, userId)
        locationTracker.startTracking()
    }

    override fun onStop() {
        super.onStop()
        if (::locationTracker.isInitialized) {
            locationTracker.stopTracking()
        }
    }

    private fun shouldShowSOS(): Boolean {
        val ageStr = sharedPrefs.getString("AGE", null)
        val gender = sharedPrefs.getString("GENDER", null)
        val age = ageStr?.toIntOrNull()
        return gender.equals("female", ignoreCase = true) || (age != null && age < 18)
    }

    private fun checkAllPermissions() {
        val allRequired = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) + REQUIRED_SOS_PERMISSIONS

        val missing = allRequired.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                SOS_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun triggerSOS() {
        Toast.makeText(this, "🚨 SOS Activated!", Toast.LENGTH_LONG).show()

        // Call 100
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:100")))
        }

        // Record video
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30)  // 30 seconds
            }
            startActivity(intent)
        }

        // SMS all contacts
        sendAlertToFriends()
    }

    private fun sendAlertToFriends() {
        val userId = sharedPrefs.getString("USER_ID", null) ?: return
        val name = sharedPrefs.getString("NAME", "Someone")
        val lat = sharedPrefs.getString("LATITUDE", null)
        val lon = sharedPrefs.getString("LONGITUDE", null)

        val locationText = if (lat != null && lon != null)
            "https://www.google.com/maps?q=$lat,$lon"
        else
            "Location unavailable"

        val message = "🚨 SOS ALERT: $name needs help! Last known location: $locationText"

        if (NetworkUtils.isInternetAvailable(this)) {
            // Internet available — push Firebase alert AND send SMS
            val db = FirebaseDatabase.getInstance(
                "https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app/"
            )
            db.getReference("user-shared-locations").child(userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "No contacts to alert.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    var sentCount = 0
                    val phonesToSms = mutableListOf<String>()

                    for (contactSnap in snapshot.children) {
                        if (contactSnap.key == "status") continue
                        val phone = contactSnap.child("phone")
                            .getValue(String::class.java) ?: continue
                        phonesToSms.add(phone)

                        // Also push a Firebase alert node so friends see it in app
                        db.getReference("sos_alerts").child(userId).setValue(
                            mapOf(
                                "message" to message,
                                "timestamp" to System.currentTimeMillis(),
                                "lat" to (lat ?: ""),
                                "lon" to (lon ?: "")
                            )
                        )
                    }

                    // SMS all contacts even when internet is available
                    // so they get alerted even if they don't have the app open
                    phonesToSms.forEach { phone ->
                        if (ContextCompat.checkSelfPermission(
                                this, Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            try {
                                SmsManager.getDefault()
                                    .sendTextMessage(phone, null, message, null, null)
                                sentCount++
                            } catch (e: Exception) {
                                android.util.Log.e("SOS", "Failed to SMS $phone: ${e.message}")
                            }
                        }
                    }

                    if (sentCount > 0) {
                        Toast.makeText(
                            this,
                            "SOS sent to $sentCount contact${if (sentCount > 1) "s" else ""}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this, "No contacts could be reached.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    // Firebase failed even though internet seemed available
                    // fall back to SMS only
                    sendSmsToLocalContacts(message)
                }
        } else {
            // No internet — SMS only using locally stored contacts
            sendSmsToLocalContacts(message)
        }
    }

    // SMS fallback using contacts stored locally in SharedPreferences
    private fun sendSmsToLocalContacts(message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "SMS permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }

        // Read cached contacts from local SharedPreferences
        // These were saved when the user loaded their contacts in FriendsFragment
        val contactPrefs = getSharedPreferences("ContactCounts", MODE_PRIVATE)
        val allEntries = contactPrefs.all

        if (allEntries.isEmpty()) {
            Toast.makeText(this, "No local contacts found for SMS fallback.", Toast.LENGTH_SHORT).show()
            return
        }

        var sentCount = 0
        allEntries.keys.forEach { key ->
            // Keys are stored as "count_+911234567890" — extract the phone number
            if (key.startsWith("count_")) {
                val phone = key.removePrefix("count_")
                try {
                    SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
                    sentCount++
                } catch (e: Exception) {
                    android.util.Log.e("SOS", "SMS fallback failed for $phone: ${e.message}")
                }
            }
        }

        Toast.makeText(
            this,
            if (sentCount > 0) "📵 No internet — SOS SMS sent to $sentCount contact${if (sentCount > 1) "s" else ""}"
            else "Could not reach any contacts.",
            Toast.LENGTH_LONG
        ).show()
    }
    private fun sendSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
        }
    }

    fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SOS_PERMISSIONS_REQUEST_CODE) {
            val denied = permissions.indices.filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
            }
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val userId = sharedPrefs.getString("USER_ID", null) ?: return
            locationTracker = LocationTracker(this, userId)
            locationTracker.startTracking()
        }
    }
}

// Navigation Items
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
    object Friends : BottomNavItem("friends", Icons.Default.Group, "Friends")
}

@Composable
fun HomeScreen(
    showSOSButton: Boolean,
    onSOSClick: () -> Unit,
    fragmentManager: FragmentManager,
    sharedPrefs: SharedPreferences
) {
    var selectedTab by remember { mutableStateOf(0) }
    val navItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Profile,
        BottomNavItem.Friends
    )

    Scaffold(
        containerColor = Color(0xFF2B2D31),
        bottomBar = {
            ModernBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    when (index) {
                        0 -> loadHomeFragment(fragmentManager)
                        1 -> loadProfileFragment(fragmentManager, sharedPrefs)
                        2 -> loadFriendsFragment(fragmentManager)
                    }
                },
                items = navItems
            )
        },
        floatingActionButton = {
            if (showSOSButton) {
                SOSButton(onClick = onSOSClick)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fragment Container
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = R.id.fragmentContainer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Load initial fragment
    LaunchedEffect(Unit) {
        loadHomeFragment(fragmentManager)
    }
}

@Composable
fun ModernBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<BottomNavItem>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        color = Color(0xFF1E1F22),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BottomNavTab(
                    icon = item.icon,
                    label = item.label,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
fun BottomNavTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFFB8500) else Color(0xFF6D6F78),
        animationSpec = tween(300),
        label = "tab_color"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(28.dp * animatedScale)
            )
            if (selected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
fun SOSButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "sos_scale"
    )

    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .size(64.dp)
            .shadow(8.dp, CircleShape),
        containerColor = Color(0xFFD00000),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "SOS",
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "SOS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper functions to load fragments
private fun loadHomeFragment(fragmentManager: FragmentManager) {
    fragmentManager.beginTransaction()
        .replace(R.id.fragmentContainer, HomeFragment())
        .commit()
}

private fun loadProfileFragment(fragmentManager: FragmentManager, sharedPrefs: SharedPreferences) {
    val fragment = ProfileFragment().apply {
        arguments = Bundle().apply {
            putString("NAME", sharedPrefs.getString("NAME", ""))
            putString("AGE", sharedPrefs.getString("AGE", ""))
            putString("GENDER", sharedPrefs.getString("GENDER", ""))
            putString("PHONE_NO", sharedPrefs.getString("PHONE_NO", ""))
        }
    }
    fragmentManager.beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .commit()
}

private fun loadFriendsFragment(fragmentManager: FragmentManager) {
    fragmentManager.beginTransaction()
        .replace(R.id.fragmentContainer, FriendsFragment())
        .commit()
}