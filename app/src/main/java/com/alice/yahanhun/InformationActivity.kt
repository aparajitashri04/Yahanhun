package com.alice.yahanhun
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alice.yahahun.R
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class InformationActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    fun writeNewUser(userId: String, name: String, phoneNumber: String) {
        val user = User(name, phoneNumber)

        var database = FirebaseDatabase.getInstance("https://yahan-hun-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        database.child("users").child(userId).setValue(user)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // ✅ Important
        setContentView(R.layout.layout_information_activity)
        val userId = null
        val Database = null

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val ageInput = findViewById<EditText>(R.id.ageInput)
        val genderInput = findViewById<EditText>(R.id.genderInput)
        val next = findViewById<Button>(R.id.next)

        val phoneNumber = intent.getStringExtra("PHONE_NO") ?: ""

        database = FirebaseDatabase.getInstance().reference


        next.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val age = ageInput.text.toString().trim()
            val gender = genderInput.text.toString().trim()
            Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show()
            writeNewUser("123","Alice","20",)
        }
    }
    data class User(val name: String, val email: String) {

    }

}

private fun Unit.setValue(informationActivityUser: InformationActivity.User) {
    TODO("Not yet implemented")
}

private fun Nothing?.child(string: String) {
    TODO("Not yet implemented")
}






