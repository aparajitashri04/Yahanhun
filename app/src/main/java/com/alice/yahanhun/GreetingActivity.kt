package com.alice.yahanhun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alice.yahanhun.ui.theme.YahanHunTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class GreetingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val name = sharedPrefs.getString("NAME", "there") ?: "there"
        val userId = sharedPrefs.getString("USER_ID", null)

        sharedPrefs.edit().putBoolean("GREETING_SHOWN", true).apply()

        setContent {
            YahanHunTheme {
                GreetingScreen(
                    name = name,
                    onFinished = {
                        val intent = Intent(
                            this,
                            if (userId != null) HomeActivity::class.java
                            else MainActivity::class.java
                        )
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

private data class GreetingStyle(
    val greeting: String,
    val backgroundColors: List<Color>,
    val accentColor: Color
)

private fun getGreetingStyle(hour: Int): GreetingStyle {
    return when (hour) {
        in 5..11 -> GreetingStyle(
            greeting = "Good Morning",
            backgroundColors = listOf(
                Color(0xFF1a1a2e),
                Color(0xFF2d1b00),
                Color(0xFF3d2800)
            ),
            accentColor = Color(0xFFFFDB58)
        )
        in 12..16 -> GreetingStyle(
            greeting = "Good Afternoon",
            backgroundColors = listOf(
                Color(0xFF1a1a2e),
                Color(0xFF2d1500),
                Color(0xFF3d2000)
            ),
            accentColor = Color(0xFFFB8500)
        )
        else -> GreetingStyle(
            greeting = "Good Evening",
            backgroundColors = listOf(
                Color(0xFF1a1a2e),
                Color(0xFF16213e),
                Color(0xFF0f3460)
            ),
            accentColor = Color(0xFFB5BAC1)
        )
    }
}

@Composable
fun GreetingScreen(
    name: String,
    onFinished: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val style = remember { getGreetingStyle(hour) }

    val alpha = remember { Animatable(0f) }
    val nameOffset = remember { Animatable(40f) }
    val nameAlpha = remember { Animatable(0f) }
    var visibleDots by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
        }

        delay(400)

        launch {
            nameOffset.animateTo(0f, animationSpec = tween(500, easing = EaseOutCubic))
        }
        launch {
            nameAlpha.animateTo(1f, animationSpec = tween(500))
        }

        delay(300)
        repeat(3) { i ->
            visibleDots = i + 1
            delay(300)
        }

        delay(600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = style.backgroundColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Greeting
            Text(
                text = style.greeting,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = style.accentColor,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(alpha.value)
            )

            // Name slides up
            Box(
                modifier = Modifier
                    .offset(y = nameOffset.value.dp)
                    .alpha(nameAlpha.value)
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Loading dots
            Spacer(modifier = Modifier.height(60.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(nameAlpha.value)
            ) {
                repeat(3) { index ->
                    val dotScale by animateFloatAsState(
                        targetValue = if (index < visibleDots) 1f else 0.5f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(dotScale)
                            .alpha(if (index < visibleDots) 1f else 0.2f)
                            .background(
                                color = if (index < visibleDots) style.accentColor
                                else Color(0xFFB5BAC1),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}