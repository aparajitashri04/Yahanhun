package com.alice.yahanhun.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.alice.yahanhun.LoginActivity
import com.alice.yahanhun.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPrefs = requireActivity()
            .getSharedPreferences("UserSession", AppCompatActivity.MODE_PRIVATE)
        val contactPrefs = requireActivity()
            .getSharedPreferences("ContactCounts", AppCompatActivity.MODE_PRIVATE)

        val name = sharedPrefs.getString("NAME", "N/A") ?: "N/A"
        val age = sharedPrefs.getString("AGE", "N/A") ?: "N/A"
        val gender = sharedPrefs.getString("GENDER", "N/A") ?: "N/A"
        val phone = sharedPrefs.getString("PHONE", "N/A") ?: "N/A"
        val savedImagePath = sharedPrefs.getString("PROFILE_IMAGE_PATH", null)

        // Reset counts at 5am
        val now = Calendar.getInstance()
        val lastReset = contactPrefs.getLong("LAST_RESET", 0L)
        val lastResetCal = Calendar.getInstance().also { it.timeInMillis = lastReset }
        val todayAt5am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 5)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now.after(todayAt5am) && lastResetCal.before(todayAt5am)) {
            val editor = contactPrefs.edit()
            contactPrefs.all.keys
                .filter { it.startsWith("count_") }
                .forEach { editor.remove(it) }
            editor.putLong("LAST_RESET", now.timeInMillis)
            editor.apply()
        }

        val totalShared = contactPrefs.all.entries
            .filter { it.key.startsWith("count_") }
            .sumOf { (it.value as? Int) ?: 0 }

        val friendsCount = contactPrefs.all.keys
            .count { it.startsWith("count_") }

        return ComposeView(requireContext()).apply {
            setContent {
                YahanHunTheme {
                    ProfileScreen(
                        name = name,
                        age = age,
                        gender = gender,
                        phone = phone,
                        totalShared = totalShared,
                        friendsCount = friendsCount,
                        savedImagePath = savedImagePath,
                        onImagePathSaved = { path ->
                            sharedPrefs.edit()
                                .putString("PROFILE_IMAGE_PATH", path)
                                .apply()
                        },
                        onLogout = {
                            sharedPrefs.edit().clear().apply()
                            val intent = Intent(requireActivity(), LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "profile_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("ProfileFragment", "Failed to save image: ${e.message}")
        null
    }
}

@Composable
fun ProfileScreen(
    name: String,
    age: String,
    gender: String,
    phone: String,
    totalShared: Int,
    friendsCount: Int,
    savedImagePath: String?,
    onImagePathSaved: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var profileImageUri by remember {
        mutableStateOf(
            savedImagePath?.let { path ->
                val file = File(path)
                if (file.exists()) Uri.fromFile(file) else null
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                profileImageUri = Uri.fromFile(File(savedPath))
                onImagePathSaved(savedPath)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            var avatarVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { avatarVisible = true }

            AnimatedVisibility(
                visible = avatarVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFB8500),
                                            Color(0xFFFF9A3C)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Camera hint at bottom
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFB8500)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Profile Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    ProfileInfoItem(label = "NAME", value = name)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileInfoItem(label = "AGE", value = age)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileInfoItem(label = "GENDER", value = gender)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileInfoItem(label = "PHONE NUMBER", value = phone)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2B2D31).copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.LocationOn,
                        value = "Active",
                        label = "Tracking"
                    )
                    StatItem(
                        icon = Icons.Default.People,
                        value = friendsCount.toString(),
                        label = "Contacts"
                    )
                    StatItem(
                        icon = Icons.Default.Share,
                        value = totalShared.toString(),
                        label = "Shared"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2B2D31)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color(0xFFFB8500),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(text = "Log Out", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to log out?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFB8500)
                        )
                    ) {
                        Text("Yes, Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color(0xFF6D6F78))
                    }
                },
                containerColor = Color(0xFF2B2D31),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFB5BAC1)
            )
        }
    }
}

@Composable
fun ProfileInfoItem(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1a1a1a),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFFB8500),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFB5BAC1)
        )
    }
}