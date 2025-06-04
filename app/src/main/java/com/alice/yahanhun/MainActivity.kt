package com.alice.yahanhun
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.alice.yahahun.R
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlin.jvm.java
import com.google.firebase.database.DatabaseReference

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.layout_main_activity)


        val phoneNumber = findViewById<EditText>(R.id.phoneNumber)
        val signUpbutton = findViewById<Button>(R.id.signUpButton)
        database = FirebaseDatabase.getInstance().reference


        signUpbutton.setOnClickListener {
            val phoneNumber = phoneNumber.text.toString()
            val intent = Intent(this, InformationActivity::class.java)
            intent.putExtra("PHONE_NO", phoneNumber)
            startActivity(intent)
        }
    }
}


