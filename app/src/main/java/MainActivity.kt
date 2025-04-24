/* package com.example.frikidates

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        //etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        btnRegister = findViewById(R.id.btn_register)

        val viewSwitcher = findViewById<ViewSwitcher>(R.id.viewSwitcher)
        findViewById<TextView>(R.id.tv_go_to_register).setOnClickListener {
            viewSwitcher.showNext()
        }
        findViewById<TextView>(R.id.tv_go_to_login).setOnClickListener {
            viewSwitcher.showPrevious()
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val db = FirebaseFirestore.getInstance()

                        val userData = hashMapOf(
                            "name" to name,
                            "email" to email
                        )

                        // Contar documentos actuales en 'user' para generar nuevo ID
                        db.collection("user").get()
                            .addOnSuccessListener { userDocs ->
                                val newIdNumber = userDocs.size() + 1
                                val docId = "user_$newIdNumber"

                                // Crear documento en 'user'
                                db.collection("user").document(docId).set(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Usuario guardado correctamente", Toast.LENGTH_SHORT).show()

                                        // Crear perfil en 'profiles' con el mismo ID
                                        val perfilData = hashMapOf(
                                            "edad" to 25,
                                            "intereses" to listOf("anime", "videojuegos"),
                                            "bio" to "Fan de manga y sci-fi"
                                        )

                                        db.collection("profiles").document("profile_$newIdNumber").set(perfilData)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error al guardar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                            }

                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al guardar usuario: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al contar usuarios: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

    }
}
*/