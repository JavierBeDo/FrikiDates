package com.example.frikidates

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
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
    private lateinit var tvAgeRangeMin: TextView
    private lateinit var tvAgeRangeMax: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var llMoreWords: LinearLayout
    private lateinit var ivExpand: ImageView
    private lateinit var birthdateDisplay: TextView
    private lateinit var descEdit2 : EditText
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

        val db = FirebaseFirestore.getInstance()
        val interestsRef = db.collection("interests")

        interestsRef.get().addOnCompleteListener { task: Task<QuerySnapshot> ->
            if (task.isSuccessful) {
                for (document in task.result) {
                    // Obtener el grupo familiar
                    val groupName = document.id
                    val names =
                        document["name"] as List<String>? // Asumiendo que los nombres están en un campo "name"

                    // Llamar a la función para agregar el grupo y sus intereses
                    if (names != null) {
                        addGroupToLayout(groupName, names)
                    }
                }
            } else {
                Log.d("Firebase", "Error getting documents: ", task.exception)
            }
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
        descEdit2 = findViewById(R.id.descEdit2)

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
            val email = etEmail_registrer.text.toString()
            val password = etPassword_registrer.text.toString()
            val gender = spinnerGender.selectedItem.toString()
            val genderPref = spinnerGenderPref.selectedItem.toString()
            val ageRange = "${tvAgeRangeMin.text}-${tvAgeRangeMax.text}"
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

                    // Recoger intereses seleccionados
                    val intereses = mutableListOf<String>()
                    for (i in 0 until llMoreWords.childCount) {
                        val view = llMoreWords.getChildAt(i)
                        if (view is TextView && view.currentTextColor == resources.getColor(R.color.verde_seleccionado)) {
                            intereses.add(view.text.toString())
                        }
                    }

                    val profileId = "profile_$uid"

                    // Datos del documento user/{uid}
                    val userData = hashMapOf(
                        "status" to "active",
                        "profileId" to "profiles/$profileId"
                    )

                    // Datos del documento profiles/profile_$uid
                    val perfilData = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "email" to email,
                        "birthdate" to birthdate,
                        "edad" to age,
                        "genero" to gender,
                        "preferenciaGenero" to genderPref,
                        "rangoEdadBuscado" to ageRange,
                        "distanciaMax" to 50,
                        "intereses" to intereses,
                        "bio" to desc,
                        "imgUrl" to ""
                    )

                    // Guardar en Firestore
                    db.collection("profiles").document(profileId).set(perfilData)
                        .addOnSuccessListener {
                            db.collection("user").document(uid).set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                                    saveUserToPreferences(uid)
                                    startActivity(Intent(this, AddphotosActivity::class.java))
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

    private fun addGroupToLayout(groupName: String, names: List<String>) {
        val llInterestVertical: LinearLayout = findViewById(R.id.ll_interest_vertical)
        val ivExpand: ImageView = findViewById(R.id.iv_expand)

        // Crear el título para el grupo
        val groupTitle = TextView(this).apply {
            text = groupName  // El nombre del grupo
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }
        llInterestVertical.addView(groupTitle)

        // Crear los TextViews para los primeros 4 intereses
        val maxVisible = 4
        for (i in 0 until minOf(names.size, maxVisible)) {
            val interestTextView = TextView(this).apply {
                text = names[i]  // El nombre del interés
                textSize = 16f
                setPadding(10, 10, 10, 10)
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.circle_background) // Fondo circular
            }
            llInterestVertical.addView(interestTextView)
        }

        // Crear los TextViews para los intereses adicionales (ocultos)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16  // Márgenes entre los grupos
        }

        for (i in maxVisible until names.size) {
            val interestTextView = TextView(this).apply {
                text = names[i]  // El nombre del interés
                textSize = 16f
                setPadding(10, 10, 10, 10)
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.circle_background) // Fondo circular
            }
            llInterestVertical.addView(interestTextView, params)
        }

        // Ocultar los intereses adicionales inicialmente
        if (names.size > maxVisible) {
            for (i in maxVisible until names.size) {
                llInterestVertical.getChildAt(i).visibility = View.GONE
            }
        }

        // Agregar la lógica para expandir/colapsar
        ivExpand.setOnClickListener {
            val isVisible = llInterestVertical.getChildAt(maxVisible).visibility == View.VISIBLE
            for (i in maxVisible until names.size) {
                llInterestVertical.getChildAt(i).visibility =
                    if (isVisible) View.GONE else View.VISIBLE
            }
            ivExpand.rotation = if (isVisible) 0f else 180f // Rotar la flecha
        }
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
