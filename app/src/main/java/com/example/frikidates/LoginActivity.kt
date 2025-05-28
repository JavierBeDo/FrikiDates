package com.example.frikidates

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.firebase.FirebaseRepository
import java.util.Calendar
import com.google.android.material.slider.RangeSlider

class LoginActivity : AppCompatActivity() {

    private lateinit var viewSwitcher: ViewSwitcher
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var tvResendVerification: TextView
    private lateinit var mAuth: FirebaseAuth


    // Registro
    private lateinit var etName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etEmailRegister: EditText
    private lateinit var etPasswordRegister: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerGenderPref: Spinner
    private lateinit var ageRangeBar: RangeSlider
    private lateinit var seekBarDistancia: SeekBar
    private lateinit var tvAgeRangeMin: TextView
    private lateinit var tvAgeRangeMax: TextView
    private lateinit var tvDistRangeMin: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var birthdateDisplay: TextView
    private lateinit var descEdit: EditText
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(this)
        val user = userPreferences.getUser()

        if (user != null) {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
            return
        } else {
            setContentView(R.layout.activity_login)
        }

        viewSwitcher = findViewById(R.id.viewSwitcher)
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        btnLogin = findViewById(R.id.btn_login)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)
        tvResendVerification = findViewById(R.id.tv_resend_mail)

        mAuth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.et_register_name)
        etSurname = findViewById(R.id.et_register_surname)
        etEmailRegister = findViewById(R.id.et_register_email)
        etPasswordRegister = findViewById(R.id.et_register_password)
        spinnerGender = findViewById(R.id.spinner3)
        spinnerGenderPref = findViewById(R.id.spinner4)
        ageRangeBar = findViewById(R.id.age_range_slider)
        tvAgeRangeMin = findViewById(R.id.tv_edadmin)
        tvAgeRangeMax = findViewById(R.id.tv_edadmax)
        seekBarDistancia = findViewById(R.id.seekBarDistancia)
        tvDistRangeMin = findViewById(R.id.tvDistRangeMin)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)
        birthdateDisplay = findViewById(R.id.birthdate_display)
        descEdit = findViewById(R.id.descEdit2)

        //Perdí la contraseña
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        setupAgeRangeBar()
        setupUI()
    }

    private fun setupUI() {
        // Login button
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user ?: return@addOnSuccessListener

                    if (!user.isEmailVerified) {
                        Toast.makeText(this, "Verifica tu correo antes de iniciar sesión", Toast.LENGTH_LONG).show()

                        // Reenvío automático
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Correo de verificación reenviado", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al reenviar el correo de verificación", Toast.LENGTH_SHORT).show()
                            }

                        // Mostrar botón de reenvío manual
                        tvResendVerification.visibility = View.VISIBLE

                        FirebaseAuth.getInstance().signOut()
                        return@addOnSuccessListener
                    }

                    val uid = user.uid
                    val db = FirebaseFirestore.getInstance()

                    db.collection("user").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val profileId = userDoc.getString("profileId")

                            if (profileId != null) {
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
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString()
                val password = etPassword.text.toString()

                if (email.isEmpty() || password.isEmpty()) {
                    showToast(getString(R.string.fill_all_fields))
                    return@setOnClickListener
                }

                FirebaseRepository.loginAndLoadProfile(
                    email,
                    password,
                    onSuccess = { uid ->
                        showToast(getString(R.string.login_successful))
                        saveUserToPreferences(uid)
                        startActivity(Intent(this, MainMenuActivity::class.java))
                        finish()
                    },
                    onError = { message ->
                        showToast(message)
                    }
                )
            }

        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Por favor, introduce un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseRepository.resetPassword(
                email,
                onSuccess = {
                    Toast.makeText(this, "Correo de recuperación enviado. Revisa tu bandeja de entrada.", Toast.LENGTH_LONG).show()
                },
                onFailure = { e ->
                    val errorMessage = when (e.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "No existe una cuenta con este correo."
                        else -> "Error al enviar el correo: ${e.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            )

        }

        // Register button
        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val surname = etSurname.text.toString()
            val email = etEmailRegister.text.toString()
            val password = etPasswordRegister.text.toString()
            val gender = spinnerGender.selectedItem.toString()
            val genderPref = spinnerGenderPref.selectedItem.toString()
            val ageRangeMin = ageRangeBar.values[0].toInt() // Obtener valor del RangeSlider
            val ageRangeMax = ageRangeBar.values[1].toInt()
            val birthdate = birthdateDisplay.text.toString()
            val desc = descEdit.text.toString()
            val age = calculateAge(birthdate)

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()
                || gender.isEmpty() || genderPref.isEmpty() || desc.isEmpty()
                || birthdate == "Selecciona tu fecha de nacimiento" || age == -1
            ) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }

            FirebaseRepository.registerUser(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user ?: return@addOnSuccessListener
                    val uid = user.uid
                    val profileId = "profile_$uid"

                    // Enviar correo de verificación
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Verifica tu correo antes de continuar", Toast.LENGTH_LONG).show()

                            // Guardar perfil, pero NO iniciar sesión
                            val perfilData = hashMapOf(
                                "name" to name,
                                "surname" to surname,
                                "email" to email,
                                "birthdate" to birthdate,
                                "genero" to gender,
                                "preferenciaGenero" to genderPref,
                                "rangoEdadBuscado" to ageRange,
                                "distanciaMax" to (5 + seekBarDistancia.progress),
                                "bio" to desc,
                                "imgUrl" to "",
                                "notificaciones" to true
                            )

                            val userData = hashMapOf(
                                "status" to "active",
                                "profileId" to profileId
                            )

                            db.collection("profiles").document(profileId).set(perfilData)
                                .addOnSuccessListener {
                                    db.collection("user").document(uid).set(userData)
                                        .addOnSuccessListener {
                                            FirebaseAuth.getInstance().signOut() // Importante: cerrar sesión
                                            viewSwitcher.showPrevious() // Volver al login
                                            Toast.makeText(this, "Registro exitoso. Revisa tu correo", Toast.LENGTH_LONG).show()
                                        }
                                }
                    val uid = authResult.user?.uid ?: return@addOnSuccessListener

                    FirebaseRepository.createUserProfile(
                        uid, name, surname, email, birthdate, gender, genderPref, ageRangeMin, ageRangeMax , desc
                    )
                        .addOnSuccessListener {
                            showToast(getString(R.string.user_registered_successfully))
                            saveUserToPreferences(uid)
                            startActivity(Intent(this, LoginInterestsActivity::class.java))
                        }
                        .addOnFailureListener { e ->
                            showToast(getString(R.string.error_saving_data, e.message))
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "No se pudo enviar el correo de verificación", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    showToast(getString(R.string.registration_error, e.message))
                }
        }




        // Switcher actions
        tvGoToRegister.setOnClickListener { viewSwitcher.showNext() }
        tvGoToLogin.setOnClickListener { viewSwitcher.showPrevious() }

        // Spinners
        val genderOptions = arrayOf("Hombre", "Mujer", "No-binario")

        val genderPrefOptions = arrayOf("Masculino", "Femenino", "Cualquiera")
        spinnerGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        spinnerGenderPref.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderPrefOptions)

        seekBarDistancia.max = 195
        seekBarDistancia.progress = 0
        tvDistRangeMin.text = "5 km"
        seekBarDistancia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistRangeMin.text = getString(R.string.km_unit, 5 + progress)

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Date picker
        birthdateDisplay.setOnClickListener { showDatePickerDialog() }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            birthdateDisplay.text = date
        }, year, month, day).show()
    }

    private fun saveUserToPreferences(userId: String) {
        val user = User(userId)
        val userPreferences = UserPreferences(this)
        userPreferences.saveUser(user)
    }

    private fun setupAgeRangeBar() {
        // Establecer valores iniciales
        ageRangeBar.values = listOf(18f, 99f)
        // Actualizar TextViews con valores iniciales
        tvAgeRangeMin.text = "Edad mínima: ${ageRangeBar.values[0].toInt()}"
        tvAgeRangeMax.text = "Edad máxima: ${ageRangeBar.values[1].toInt()}"
        // Listener para cambios en el rango
        ageRangeBar.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvAgeRangeMin.text = "Edad mínima: ${values[0].toInt()}"
            tvAgeRangeMax.text = "Edad máxima: ${values[1].toInt()}"
        }
    }
}
