package com.alice.yahanhun

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alice.yahanhun.ui.theme.YahanHunTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class InformationActivity : ComponentActivity() {

    private val database = FirebaseDatabase
        .getInstance("https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app")
        .reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val phoneNumber = intent.getStringExtra("PHONE_NO") ?: ""

        setContent {
            YahanHunTheme {
                InformationScreen(
                    onNext = { name, age, gender ->
                        val user = User(name, age, gender, phoneNumber)
                        val uid = database.child("users").push().key

                        if (uid == null) {
                            Toast.makeText(this, "Failed to generate user ID", Toast.LENGTH_SHORT).show()
                            return@InformationScreen
                        }

                        database.child("users").child(uid).setValue(user)
                            .addOnSuccessListener {
                                database.child("userPhone").child(phoneNumber).setValue(uid)

                                getSharedPreferences("UserSession", MODE_PRIVATE).edit().apply {
                                    putString("USER_ID", uid)
                                    putString("PHONE", phoneNumber)
                                    putString("NAME", name)
                                    putString("AGE", age)
                                    putString("GENDER", gender)
                                    putBoolean("SIGNED_UP", true)
                                    apply()
                                }

                                startActivity(
                                    Intent(this, PasswordActivity::class.java).apply {
                                        putExtra("USER_ID", uid)
                                        putExtra("PHONE_NO", phoneNumber)
                                        putExtra("NAME", name)
                                        putExtra("AGE", age)
                                        putExtra("GENDER", gender)
                                    }
                                )
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(
    onNext: (name: String, age: String, gender: String) -> Unit,
    onError: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAge by remember { mutableStateOf("18") }
    var selectedGender by remember { mutableStateOf("") }
    var ageDropdownExpanded by remember { mutableStateOf(false) }
    var isButtonPressed by remember { mutableStateOf(false) }

    val ages = (12..120).map { it.toString() }
    val genders = listOf("Male", "Female", "Others")

    val isFormValid = name.isNotEmpty() && selectedGender.isNotEmpty()

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
                        text = "Tell us about yourself",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "We'll use this to personalize your experience",
                        fontSize = 13.sp,
                        color = Color(0xFF1a1a1a),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Name
                    FieldLabel("NAME")
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            if (it.length <= 30) name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your name", color = Color(0xFF757575)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Name",
                                tint = Color(0xFF1E1F22)
                            )
                        },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                Text(
                                    "${name.length}/30",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Age dropdown
                    FieldLabel("AGE")
                    ExposedDropdownMenuBox(
                        expanded = ageDropdownExpanded,
                        onExpandedChange = { ageDropdownExpanded = !ageDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedAge,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = Color(0xFFB5BAC1)
                                )
                            },
                            colors = outlinedFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = ageDropdownExpanded,
                            onDismissRequest = { ageDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF2B2D31))
                        ) {
                            ages.forEach { age ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            age,
                                            color = if (age == selectedAge)
                                                Color(0xFFFB8500)
                                            else
                                                Color.White
                                        )
                                    },
                                    onClick = {
                                        selectedAge = age
                                        ageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender
                    FieldLabel("GENDER")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genders.forEach { gender ->
                            val isSelected = selectedGender == gender
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF1E1F22)
                                        else Color(0xFF1E1F22).copy(alpha = 0.6f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color(0xFFFB8500)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedGender = gender }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = gender,
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White
                                    else Color(0xFFB5BAC1)
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFFB8500),
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("✓", fontSize = 12.sp, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Next button
                    Button(
                        onClick = {
                            isButtonPressed = true
                            when {
                                name.isEmpty() || !name.trim().all { it.isLetter() || it.isWhitespace() } ->
                                    onError("Enter a valid name (letters only, max 30)")
                                selectedGender.isEmpty() ->
                                    onError("Please select a gender")
                                else -> onNext(name.trim(), selectedAge, selectedGender)
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
                            text = "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your information is kept private and secure",
                fontSize = 12.sp,
                color = Color(0xFF6D6F78),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

// Reusable helpers to keep the code DRY
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0D0D0F),
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF1E1F22),
    unfocusedContainerColor = Color(0xFF1E1F22),
    focusedBorderColor = Color(0xFFFB8500),
    unfocusedBorderColor = Color(0xFF2B2D31),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White
)