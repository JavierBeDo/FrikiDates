package com.example.frikidates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LoginActivity : AppCompatActivity() {

    private lateinit var viewSwitcher: ViewSwitcher
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail_registrer: EditText
    private lateinit var etPassword_registrer: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var tvGoToLogin: TextView
    private lateinit var mAuth: FirebaseAuth

    // Registro
    private lateinit var etName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etAge: EditText
    private lateinit var etGender: EditText
    private lateinit var etGenderPref: EditText
    private lateinit var etAgeRange: EditText
    private lateinit var btnRegister: Button
    private val db = FirebaseFirestore.getInstance()

    // Location
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        // --------- LOCATION --------
        locationManager = LocationManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        obtenerUbicacion()

        viewSwitcher = findViewById(R.id.viewSwitcher)
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        btnLogin = findViewById(R.id.btn_login)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)

        // --------- REGISTRO --------
        etName = findViewById(R.id.et_register_name)
        etSurname = findViewById(R.id.et_register_surname)
        etAge = findViewById(R.id.et_register_age)
        etEmail_registrer = findViewById(R.id.et_register_email)
        etPassword_registrer = findViewById(R.id.et_register_password)
        etGender = findViewById(R.id.et_register_gender)
        etGenderPref = findViewById(R.id.et_register_gender_preference)
        etAgeRange = findViewById(R.id.et_register_age_range)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // Acción para cambiar a la vista de registro
        tvGoToRegister.setOnClickListener {
            viewSwitcher.showNext()
        }

        // Acción para volver a la vista de login
        tvGoToLogin.setOnClickListener {
            viewSwitcher.showPrevious()
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val surname = etSurname.text.toString()
            val age = etAge.text.toString().toIntOrNull() ?: -1
            val email = etEmail_registrer.text.toString()
            val password = etPassword_registrer.text.toString()
            val gender = etGender.text.toString()
            val genderPref = etGenderPref.text.toString()
            val ageRange = etAgeRange.text.toString()

            if (name.isEmpty() || surname.isEmpty() || age == -1 || email.isEmpty() || password.isEmpty()
                || gender.isEmpty() || genderPref.isEmpty() || ageRange.isEmpty()
            ) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    db.collection("user").get()
                        .addOnSuccessListener { userDocs ->
                            val newId = "user_${userDocs.size() + 1}"
                            val userData = hashMapOf(
                                "name" to name,
                                "surname" to surname,
                                "email" to email
                            )

                            db.collection("user").document(newId).set(userData)
                                .addOnSuccessListener {
                                    val perfilData = hashMapOf(
                                        "edad" to age,
                                        "genero" to gender,
                                        "preferenciaGenero" to genderPref,
                                        "rangoEdadBuscado" to ageRange,
                                        "intereses" to listOf("anime", "videojuegos"),
                                        "bio" to "Fan de manga y sci-fi"
                                    )

                                    db.collection("profiles").document("profile_${userDocs.size() + 1}").set(perfilData)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                                            viewSwitcher.showPrevious()
                                        }
                                }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                locationManager.saveLocation(latitude, longitude) // Guardar ubicación
                Toast.makeText(this, "Ubicación guardada: $latitude, $longitude", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacion()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
