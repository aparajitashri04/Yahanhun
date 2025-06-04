package com.alice.yahanhun

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alice.yahahun.R

class PasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_password_activity)

        val password = findViewById<EditText>(R.id.passwordId)
        val confirmPassword = findViewById<EditText>(R.id.passwordIdConfirm)
        val submitButton = findViewById<Button>(R.id.submit)

        submitButton.setOnClickListener {
            val passwordText = password.text.toString()
            val confirmPasswordText = confirmPassword.text.toString()

            if (passwordText != confirmPasswordText) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(passwordText)) {
                Toast.makeText(
                    this,
                    "Password must contain at least one uppercase letter, one digit, and one special character.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("password", passwordText)
            startActivity(intent)
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val regex = Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{6,}\$")
        return regex.containsMatchIn(password)
    }
}

