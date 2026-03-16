package com.alice.yahanhun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alice.yahanhun.ui.theme.*

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    topPadding: Int = 50
) {
    Column(
        modifier = modifier.padding(top = topPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Yahan Hun",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stay Connected, Stay Found",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun OrangeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextOnOrange,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        InputLabel(label)

        OutlinedTextField(
            value = value,
            onValueChange = {
                if (maxLength == null || it.length <= maxLength) {
                    onValueChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "👁️" else "👁️‍🗨️")
                    }
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = AccentOrange,
                unfocusedBorderColor = InputBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ButtonBackground,
            disabledContainerColor = ButtonBackground.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.02.sp
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0D0D0F)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextOnOrange
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp)
    )
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextOnOrange
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            color = TextPrimary
        )
    }
}