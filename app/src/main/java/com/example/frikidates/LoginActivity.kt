package com.example.frikidates

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.firebase.FirebaseRepository
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class LoginActivity : AppCompatActivity() {

    private lateinit var viewSwitcher: ViewSwitcher
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var tvResendVerification: TextView
    private lateinit var tvForgotPassword: TextView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FirebaseRepository
        FirebaseRepository.init(this)

        val userPreferences = UserPreferences(this)
        val user = userPreferences.getUser()

        if (user != null) {
            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
            return
        } else {
            setContentView(R.layout.activity_login)
        }

        // Initialize views
        viewSwitcher = findViewById(R.id.viewSwitcher)
        etEmail = findViewById(R.id.et_login_email)
        etPassword = findViewById(R.id.et_login_password)
        btnLogin = findViewById(R.id.btn_login)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)
        tvResendVerification = findViewById(R.id.tv_resend_mail)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)

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

        setupAgeRangeBar()
        setupUI()
    }

    private fun setupUI() {
        // Login button
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }

            FirebaseRepository.loginAndLoadProfile(
                email,
                password,
                onSuccess = { uid ->
                    if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == false) {
                        FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                showToast("Correo de verificación enviado. Revisa tu bandeja.")
                                tvResendVerification.visibility = View.VISIBLE
                            }
                            ?.addOnFailureListener {
                                showToast("Error al enviar correo de verificación.")
                            }
                        FirebaseAuth.getInstance().signOut()
                        showToast("Verifica tu correo antes de iniciar sesión.")
                        return@loginAndLoadProfile
                    }

                    showToast(getString(R.string.login_successful))
                    saveUserToPreferences(uid)
                    startActivity(Intent(this, LoginInterestsActivity::class.java))
                    finish()
                },
                onError = { message ->
                    showToast(message)
                }
            )
        }

        // Forgot password
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showToast("Por favor, introduce tu correo electrónico.")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Por favor, introduce un correo válido.")
                return@setOnClickListener
            }

            FirebaseRepository.resetPassword(
                email,
                onSuccess = {
                    showToast("Correo de recuperación enviado. Revisa tu bandeja de entrada.")
                },
                onFailure = { e ->
                    val errorMessage = when (e.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "No existe una cuenta con este correo."
                        else -> "Error al enviar el correo: ${e.message}"
                    }
                    showToast(errorMessage)
                }
            )
        }

        // Register button
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val email = etEmailRegister.text.toString().trim()
            val password = etPasswordRegister.text.toString().trim()
            val gender = spinnerGender.selectedItem.toString()
            val genderPref = spinnerGenderPref.selectedItem.toString()
            val ageRangeMin = ageRangeBar.values[0].toInt()
            val ageRangeMax = ageRangeBar.values[1].toInt()
            val birthdate = birthdateDisplay.text.toString()
            val desc = descEdit.text.toString().trim()
            val distanciaMax = 5 + seekBarDistancia.progress
            val age = calculateAge(birthdate)

            if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty() ||
                gender.isEmpty() || genderPref.isEmpty() || desc.isEmpty() ||
                birthdate == "Selecciona tu fecha de nacimiento" || age == -1
            ) {
                showToast(getString(R.string.fill_all_fields))
                return@setOnClickListener
            }

            FirebaseRepository.registerUser(email, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user ?: return@addOnSuccessListener
                    val uid = user.uid

                    // Send verification email
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            // Create user profile
                            FirebaseRepository.createUserProfile(
                                uid, name, surname, email, birthdate, gender, genderPref,
                                ageRangeMin, ageRangeMax, desc, distanciaMax
                            ).addOnSuccessListener {
                                showToast("Registro exitoso. Revisa tu correo para verificar tu cuenta.")
                                FirebaseAuth.getInstance().signOut()
                                viewSwitcher.showPrevious() // Return to login
                            }.addOnFailureListener { e ->
                                showToast(getString(R.string.error_saving_data, e.message))
                            }
                        }
                        .addOnFailureListener {
                            showToast("No se pudo enviar el correo de verificación.")
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

        // SeekBar for distance
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
        val day = parts[0].toIntOrNull() ?: return -1
        val month = parts[1].toIntOrNull() ?: return -1
        val year = parts[2].toIntOrNull() ?: return -1

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
        ageRangeBar.values = listOf(18f, 99f)
        tvAgeRangeMin.text = "Edad mínima: ${ageRangeBar.values[0].toInt()}"
        tvAgeRangeMax.text = "Edad máxima: ${ageRangeBar.values[1].toInt()}"
        ageRangeBar.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            tvAgeRangeMin.text = "Edad mínima: ${values[0].toInt()}"
            tvAgeRangeMax.text = "Edad máxima: ${values[1].toInt()}"
        }
    }
}