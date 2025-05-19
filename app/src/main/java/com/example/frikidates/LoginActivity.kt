package com.example.frikidates

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar


class LoginActivity : AppCompatActivity() {

    private lateinit var viewSwitcher: ViewSwitcher
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var mAuth: FirebaseAuth

    // Registro
    private lateinit var etName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etEmail_registrer: EditText
    private lateinit var etPassword_registrer: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerGenderPref: Spinner
    private lateinit var seekBarAgeRange: SeekBar

    private lateinit var seekBarDistancia: SeekBar
    private lateinit var tvDistRangeMin: TextView
    private lateinit var tvAgeRangeMin: TextView
    private lateinit var tvAgeRangeMax: TextView

    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var birthdateDisplay: TextView
    private lateinit var descEdit2 : EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(this)
        val user = userPreferences.getUser()

        if (user != null) {
            // Redirigir a la actividad principal
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        } else {
            setContentView(R.layout.activity_login)
        }

        val db = FirebaseFirestore.getInstance()


        viewSwitcher = findViewById(R.id.viewSwitcher)
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        btnLogin = findViewById(R.id.btn_login)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)
        mAuth = FirebaseAuth.getInstance()

        // -------- REGISTRO --------
        etName = findViewById(R.id.et_register_name)
        etSurname = findViewById(R.id.et_register_surname)
        etEmail_registrer = findViewById(R.id.et_register_email)
        etPassword_registrer = findViewById(R.id.et_register_password)
        spinnerGender = findViewById(R.id.spinner3)
        spinnerGenderPref = findViewById(R.id.spinner4)
        seekBarAgeRange = findViewById(R.id.seekBar)
        tvAgeRangeMin = findViewById(R.id.tv_edadmin)
        tvDistRangeMin = findViewById(R.id.tvDistRangeMin)
        tvAgeRangeMax = findViewById(R.id.tv_edadmax)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)
        birthdateDisplay = findViewById(R.id.birthdate_display)
        descEdit2 = findViewById(R.id.descEdit2)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    val db = FirebaseFirestore.getInstance()

                    // Recuperar el documento del usuario para extraer el profileId (solo el ID, sin la ruta completa)
                    db.collection("user").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val profileId = userDoc.getString("profileId") // Ejemplo: "profile_lzS2rkMz..."

                            if (profileId != null) {
                                // Obtener los datos del perfil
                                db.collection("profiles").document(profileId).get()
                                    .addOnSuccessListener { profileDoc ->
                                        if (profileDoc.exists()) {
                                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                            saveUserToPreferences(uid)
                                            startActivity(Intent(this, LoginInterestsActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Perfil no encontrado", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al obtener perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Toast.makeText(this, "No se encontró el ID del perfil", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al obtener usuario: ${e.message}", Toast.LENGTH_LONG).show()
                        }
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
            val email = etEmail_registrer.text.toString()
            val password = etPassword_registrer.text.toString()
            val gender = spinnerGender.selectedItem.toString()
            val genderPref = spinnerGenderPref.selectedItem.toString()
            val ageRange = "${tvAgeRangeMin.text}-99+"
            val birthdate = birthdateDisplay.text.toString()
            val desc = descEdit2.text.toString()
            val age = calculateAge(birthdate)

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()
                || gender.isEmpty() || genderPref.isEmpty() || desc.isEmpty()
                || birthdate == "Selecciona tu fecha de nacimiento" || age == -1
            ) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    val profileId = "profile_$uid"

                    // Datos del documento user/{uid}
                    val userData = hashMapOf(
                        "status" to "active",
                        "profileId" to profileId // ✅ Solo el ID, sin "profiles/"
                    )

                    // Datos del documento profiles/profile_$uid
                    val perfilData = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "email" to email,
                        "birthdate" to birthdate,
                        "genero" to gender,
                        "preferenciaGenero" to genderPref,
                        "rangoEdadBuscado" to ageRange,
                        "distanciaMax" to 50,
                        "bio" to desc,
                        "imgUrl" to "",
                        "notificaciones" to true
                    )

                    // Guardar en Firestore
                    db.collection("profiles").document(profileId).set(perfilData)
                        .addOnSuccessListener {
                            db.collection("user").document(uid).set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                                    saveUserToPreferences(uid)
                                    startActivity(Intent(this, LoginInterestsActivity::class.java))
                                }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }


        // Configurar los Spinners
        val genderOptions = arrayOf("Masculino", "Femenino", "Otro")
        val genderPrefOptions = arrayOf("Masculino", "Femenino", "Cualquiera")
        spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        spinnerGenderPref.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderPrefOptions)

        // Configurar el SeekBar de rango de edad
        seekBarAgeRange.max = 81 // Porque 99 - 18 = 81 posibles valores
        seekBarAgeRange.progress = 0
        tvAgeRangeMin.text = "18"
        tvAgeRangeMax.text = "99+"

        seekBarAgeRange.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minAge = 18 + progress
                tvAgeRangeMin.text = minAge.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })



        // Acción para mostrar el DatePickerDialog
        birthdateDisplay.setOnClickListener { showDatePickerDialog() }

        seekBarDistancia = findViewById(R.id.seekBarDistancia)
        tvDistRangeMin = findViewById(R.id.tvDistRangeMin)

        seekBarDistancia.max = 195 // 200 - 5
        seekBarDistancia.progress = 0
        tvDistRangeMin.text = "5 km"

        seekBarDistancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val distancia = 5 + progress
                tvDistRangeMin.text = "$distancia km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Formato de la fecha
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            birthdateDisplay.text = date
        }, year, month, day).show()
    }

    private fun calculateAge(birthdate: String): Int {
        val parts = birthdate.split("/")
        if (parts.size != 3) return -1
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()

        val today = Calendar.getInstance()
        val dob = Calendar.getInstance()
        dob.set(year, month - 1, day)

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }


    private fun saveUserToPreferences(userId: String) {
        val user = User(userId)
        val userPreferences = UserPreferences(this)
        userPreferences.saveUser(user)
    }
}