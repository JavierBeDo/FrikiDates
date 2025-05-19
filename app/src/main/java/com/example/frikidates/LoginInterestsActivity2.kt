package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.utils.InterestManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginInterestsActivity2 : AppCompatActivity() {

    private lateinit var interestManager: InterestManager
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_interests)

        val layout = findViewById<LinearLayout>(R.id.ll_interest_vertical)
        btnRegister = findViewById(R.id.btn_register)

        interestManager = InterestManager(this, layout)

        interestManager.loadAndDisplayInterests(
            onError = {
                Toast.makeText(this, "Error al cargar intereses", Toast.LENGTH_SHORT).show()
            }
        )

        btnRegister.setOnClickListener {
            if (interestManager.selectedTags.size < 10) {
                Toast.makeText(this, "Selecciona al menos 10 gustos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val perfilData = mapOf("interests" to interestManager.selectedTags.toList())
            val profileId = "profile_$uid"

            FirebaseFirestore.getInstance().collection("profiles").document(profileId)
                .set(perfilData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Intereses guardados", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AddphotosActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
