package com.alice.yahanhun

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import com.alice.yahanhun.ui.theme.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import org.mindrot.jbcrypt.BCrypt

class LoginActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)

        val savedUserId = sharedPrefs.getString("USER_ID", null)
        if (savedUserId != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        FirebaseApp.initializeApp(this)

        database = FirebaseDatabase
            .getInstance("https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app")
            .reference.child("users")

        setContent {
            YahanHunTheme {
                LoginScreen(
                    onLogin = { phone, password ->
                        handleLogin(phone, password)
                    },
                    onSignUpClick = {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onForgotPassword = {
                        Toast.makeText(this, "Password recovery coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun handleLogin(phone: String, password: String) {
        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter phone and password", Toast.LENGTH_SHORT).show()
            return
        }

        database.orderByChild("phone").equalTo(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnap in snapshot.children) {
                            val storedHashedPassword = userSnap.child("password").getValue(String::class.java)

                            if (storedHashedPassword != null && BCrypt.checkpw(password, storedHashedPassword)) {
                                val name = userSnap.child("name").getValue(String::class.java) ?: "N/A"
                                val age = userSnap.child("age").getValue(String::class.java) ?: "N/A"
                                val gender = userSnap.child("gender").getValue(String::class.java) ?: "N/A"
                                val userId = userSnap.key ?: ""

                                sharedPrefs.edit().apply {
                                    putString("USER_ID", userId)
                                    putString("PHONE", phone)
                                    putString("NAME", name)
                                    putString("AGE", age)
                                    putString("GENDER", gender)
                                    apply()
                                }

                                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                                finish()
                                return
                            } else {
                                Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isButtonPressed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

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

            // Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    text = "📍",
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

            // Main Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFB8500)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "We're so excited to see you again!",
                        fontSize = 13.sp,
                        color = Color(0xFF1a1a1a),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Phone Number Input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                phoneNumber = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Enter phone number",
                                color = Color(0xFF757575)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = Color(0xFF1E1F22)
                            )
                        },
                        trailingIcon = {
                            if (phoneNumber.isNotEmpty()) {
                                Text(
                                    "${phoneNumber.length}/10",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1F22),
                            unfocusedContainerColor = Color(0xFF1E1F22),
                            focusedBorderColor = Color(0xFF2B2D31),
                            unfocusedBorderColor = Color(0xFF1E1F22),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Enter password",
                                color = Color(0xFF757575)
                            )
                        },
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
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF666666)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E1F22),
                            unfocusedContainerColor = Color(0xFF1E1F22),
                            focusedBorderColor = Color(0xFF2B2D31),
                            unfocusedBorderColor = Color(0xFF1E1F22),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Forgot Password Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    onForgotPassword()
                                }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Button with animation
                    val scale by animateFloatAsState(
                        targetValue = if (isButtonPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "button_scale"
                    )

                    Button(
                        onClick = {
                            isButtonPressed = true
                            isLoading = true
                            onLogin(phoneNumber, password)
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
                        enabled = phoneNumber.length == 10 && password.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Log In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sign Up Link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            fontSize = 14.sp,
                            color = Color(0xFF1a1a1a)
                        )
                        Text(
                            text = "Sign up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    onSignUpClick()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}