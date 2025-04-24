package com.example.frikidates

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    private lateinit var mAuth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.et_register_name)
        etEmail = findViewById(R.id.et_register_email)
        etPassword = findViewById(R.id.et_register_password)
        btnRegister = findViewById(R.id.btn_register)

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val userData = hashMapOf("name" to name, "email" to email)

                    db.collection("user").get()
                        .addOnSuccessListener { userDocs ->
                            val newId = "user_${userDocs.size() + 1}"

                            db.collection("user").document(newId).set(userData)
                                .addOnSuccessListener {
                                    val perfilData = hashMapOf(
                                        "edad" to 25,
                                        "intereses" to listOf("anime", "videojuegos"),
                                        "bio" to "Fan de manga y sci-fi"
                                    )

                                    db.collection("profiles").document("profile_${userDocs.size() + 1}").set(perfilData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                                            finish() // volver a Login
                                        }
                                }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
