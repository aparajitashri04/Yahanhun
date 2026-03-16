package com.alice.yahanhun.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.alice.yahanhun.ui.theme.YahanHunTheme
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FriendsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPrefs = requireActivity()
            .getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
        val userId = sharedPrefs.getString("USER_ID", "") ?: ""
        val lat = sharedPrefs.getString("LATITUDE", null)
        val lon = sharedPrefs.getString("LONGITUDE", null)

        return ComposeView(requireContext()).apply {
            setContent {
                YahanHunTheme {
                    FriendsScreen(
                        userId = userId,
                        latitude = lat,
                        longitude = lon
                    )
                }
            }
        }
    }
}

data class Contact(val name: String, val phone: String)

@Composable
fun FriendsScreen(
    userId: String,
    latitude: String?,
    longitude: String?
) {
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance(
        "https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )
    val userSharedLocationsRef = remember {
        db.getReference("user-shared-locations").child(userId)
            .also { it.child("status").setValue("active") }
    }

    val prefs = remember {
        context.getSharedPreferences("ContactCounts", AppCompatActivity.MODE_PRIVATE)
    }

    val sessionPrefs = remember {
        context.getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
    }

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    val addedPhones = remember { mutableStateListOf<String>() }
    var permissionGranted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun resortContacts() {
        contacts = contacts.sortedWith(
            compareByDescending<Contact> {
                prefs.getInt("count_${it.phone}", 0)
            }.thenBy { it.name }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) permissionGranted = true
        else Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect

        isLoading = true

        val loaded = withContext(Dispatchers.IO) {
            val result = mutableListOf<Contact>()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                val numberIdx = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx) ?: continue
                    val phone = it.getString(numberIdx)
                        ?.replace("\\s".toRegex(), "") ?: continue
                    result.add(Contact(name, phone))
                }
            }
            result
        }

        loaded.forEach { uploadContactToFirebase(userSharedLocationsRef, it) }

        contacts = loaded
            .distinctBy { it.phone }
            .sortedWith(
                compareByDescending<Contact> {
                    prefs.getInt("count_${it.phone}", 0)
                }.thenBy { it.name }
            )

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a2e))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16213e))
                .padding(20.dp)
        ) {
            Text(
                text = "Friends",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (userId.isEmpty() || latitude == null || longitude == null) {
                        Toast.makeText(
                            context,
                            "Location or user missing.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    val customLink =
                        "yahanhun://track?userId=$userId&lat=$latitude&lon=$longitude"
                    val mapsLink = "https://www.google.com/maps?q=$latitude,$longitude"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Track me live 📍 on YahanHun:\n$customLink\n\nOr view on Google Maps:\n$mapsLink"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8500)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share My Location",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFB8500),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            contacts.isEmpty() && permissionGranted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts found",
                        color = Color(0xFFB5BAC1),
                        fontSize = 15.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ContactSectionHeader("FROM YOUR CONTACTS")
                    }

                    items(contacts, key = { "${it.phone}_${it.name}" }) { contact ->
                        ContactItem(
                            contact = contact,
                            isAdded = contact.phone in addedPhones,
                            onShareClick = {
                                incrementShareCount(
                                    userSharedLocationsRef,
                                    contact,
                                    context
                                )
                                addedPhones.add(contact.phone)

                                val current = prefs.getInt("count_${contact.phone}", 0)
                                prefs.edit()
                                    .putInt("count_${contact.phone}", current + 1)
                                    .apply()

                                resortContacts()

                                // Send SMS directly to this contact
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.SEND_SMS
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val lat = sessionPrefs.getString("LATITUDE", null)
                                    val lon = sessionPrefs.getString("LONGITUDE", null)
                                    val userName = sessionPrefs.getString("NAME", "Someone")
                                    val locationText = if (lat != null && lon != null)
                                        "https://www.google.com/maps?q=$lat,$lon"
                                    else
                                        "Location unavailable"
                                    val message =
                                        "$userName is sharing their live location with you on YahanHun!\n\n$locationText"
                                    try {
                                        SmsManager.getDefault().sendTextMessage(
                                            contact.phone, null, message, null, null
                                        )
                                    } catch (e: Exception) {
                                        Log.e("FriendsFragment", "SMS failed: ${e.message}")
                                    }
                                }

                                Toast.makeText(
                                    context,
                                    "Shared with ${contact.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6C757D),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ContactItem(
    contact: Contact,
    isAdded: Boolean,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isAdded) Color(0xFF2B2D31) else Color(0xFFFB8500),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.name
                                .split(" ")
                                .take(2)
                                .joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdded) Color(0xFFB5BAC1) else Color.Black
                        )
                    }
                }

                Text(
                    text = contact.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isAdded) Color(0xFF1E1F22) else Color(0xFFFB8500),
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(
                    onClick = { if (!isAdded) onShareClick() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (isAdded) "Shared" else "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun uploadContactToFirebase(
    ref: DatabaseReference,
    contact: Contact
) {
    ref.child(contact.phone).setValue(
        mapOf("name" to contact.name, "phone" to contact.phone, "count" to 0)
    ).addOnFailureListener { e ->
        Log.e("FirebaseUpload", "Failed to upload ${contact.name}: ${e.message}")
    }
}

private fun incrementShareCount(
    ref: DatabaseReference,
    contact: Contact,
    context: android.content.Context
) {
    val countRef = ref.child(contact.phone).child("count")
    countRef.get()
        .addOnSuccessListener { snapshot ->
            val current = snapshot.getValue(Int::class.java) ?: 0
            countRef.setValue(current + 1)
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update count", Toast.LENGTH_SHORT).show()
        }
}