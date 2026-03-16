package com.alice.yahanhun

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alice.yahanhun.ui.theme.YahanHunTheme
import com.google.firebase.database.FirebaseDatabase
import org.mindrot.jbcrypt.BCrypt

class PasswordActivity : ComponentActivity() {

    private lateinit var userId: String
    private lateinit var phone: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getStringExtra("USER_ID") ?: ""
        phone = intent.getStringExtra("PHONE_NO") ?: ""

        setContent {
            YahanHunTheme {
                PasswordScreen(
                    onContinue = { password ->
                        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

                        val dbRef = FirebaseDatabase
                            .getInstance("https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("users").child(userId)

                        dbRef.child("password").setValue(hashedPassword)
                            .addOnSuccessListener {
                                val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                                val isFirstTime = sharedPrefs.getBoolean("SIGNED_UP", false)
                                sharedPrefs.edit().putBoolean("SIGNED_UP", false).apply()

                                val nextIntent = Intent(
                                    this,
                                    if (isFirstTime) GreetingActivity::class.java
                                    else HomeActivity::class.java
                                )
                                nextIntent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(nextIntent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Failed to save password: ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    onError = { msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PasswordScreen(
    onContinue: (String) -> Unit,
    onError: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isButtonPressed by remember { mutableStateOf(false) }

    val isFormValid = password.length >= 4 && confirmPassword.length >= 4

    val scale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale",
        finishedListener = { isButtonPressed = false }
    )

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
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔒",
                    fontSize = 64.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Yahan Hun",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Stay Connected, Stay Found",
                    fontSize = 14.sp,
                    color = Color(0xFFB5BAC1),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFB8500)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Secure Your Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Create a password to protect your account",
                        fontSize = 13.sp,
                        color = Color(0xFF1a1a1a),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Password label
                    Text(
                        text = "PASSWORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D0D0F),
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter password", color = Color(0xFF757575)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF1E1F22)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFFB5BAC1)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1F22),
                            unfocusedContainerColor = Color(0xFF1E1F22),
                            focusedBorderColor = Color(0xFFFB8500),
                            unfocusedBorderColor = Color(0xFF2B2D31),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Must be at least 4 characters",
                        fontSize = 12.sp,
                        color = Color(0xFF0D0D0F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Confirm password label
                    Text(
                        text = "CONFIRM PASSWORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D0D0F),
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Confirm password", color = Color(0xFF757575)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Confirm Password",
                                tint = Color(0xFF1E1F22)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    imageVector = if (confirmVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle confirm password",
                                    tint = Color(0xFFB5BAC1)
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1F22),
                            unfocusedContainerColor = Color(0xFF1E1F22),
                            focusedBorderColor = Color(0xFFFB8500),
                            unfocusedBorderColor = Color(0xFF2B2D31),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isButtonPressed = true
                            when {
                                password.length < 4 ->
                                    onError("Password must be at least 4 characters")
                                password != confirmPassword ->
                                    onError("Passwords do not match")
                                else -> onContinue(password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(scale),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2D31),
                            disabledContainerColor = Color(0xFF2B2D31).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isFormValid
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your password is encrypted and never stored in plain text",
                fontSize = 12.sp,
                color = Color(0xFF6D6F78),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}