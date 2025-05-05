package com.example.frikidates

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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
    private lateinit var etAge: EditText
    private lateinit var etEmail_registrer: EditText
    private lateinit var etPassword_registrer: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerGenderPref: Spinner
    private lateinit var seekBarAgeRange: SeekBar
    private lateinit var tvAgeRangeMin: TextView
    private lateinit var tvAgeRangeMax: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var llMoreWords: LinearLayout
    private lateinit var ivExpand: ImageView
    private lateinit var birthdateDisplay: TextView
    private var isExpanded = false
    private val db = FirebaseFirestore.getInstance()

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
        tvAgeRangeMax = findViewById(R.id.tv_edadmax)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)
        llMoreWords = findViewById(R.id.ll_more_words)
        ivExpand = findViewById(R.id.iv_expand)
        birthdateDisplay = findViewById(R.id.birthdate_display)

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
                    saveUserToPreferences("ejemplo")
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
            val gender = spinnerGender.selectedItem.toString()
            val genderPref = spinnerGenderPref.selectedItem.toString()
            val ageRange = "${tvAgeRangeMin.text}-${tvAgeRangeMax.text}"
            val birthdate = birthdateDisplay.text.toString()

            if (name.isEmpty() || surname.isEmpty() || age == -1 || email.isEmpty() || password.isEmpty()
                || gender.isEmpty() || genderPref.isEmpty() || birthdate == "Selecciona tu fecha de nacimiento"
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
                                "email" to email,
                                "birthdate" to birthdate
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
                                            saveUserToPreferences(newId)
                                            startActivity(Intent(this, AddphotosActivity::class.java))
                                        }
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
        seekBarAgeRange.max = 81
        seekBarAgeRange.progress = 18
        tvAgeRangeMin.text = "18"
        tvAgeRangeMax.text = "99+"

        seekBarAgeRange.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAgeRangeMin.text = progress.toString()
                tvAgeRangeMax.text = (99 - progress).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Acción para desplegar el resto del LinearLayout
        ivExpand.setOnClickListener {
            if (isExpanded) {
                llMoreWords.visibility = View.GONE
                ivExpand.setImageResource(R.drawable.flecha)
            } else {
                llMoreWords.visibility = View.VISIBLE
                ivExpand.setImageResource(R.drawable.flecha)
            }
            isExpanded = !isExpanded
        }

        // Acción para mostrar el DatePickerDialog
        birthdateDisplay.setOnClickListener { showDatePickerDialog() }
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

    private fun saveUserToPreferences(userId: String) {
        val user = User(userId)
        val userPreferences = UserPreferences(this)
        userPreferences.saveUser(user)
    }
}
