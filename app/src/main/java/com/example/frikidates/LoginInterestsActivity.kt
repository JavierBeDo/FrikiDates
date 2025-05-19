package com.example.frikidates

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions

class LoginInterestsActivity : AppCompatActivity() {

    private val selectedTags = mutableSetOf<String>()
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_interests)

        val db = FirebaseFirestore.getInstance()
        val interestsRef = db.collection("interests")
        btnRegister = findViewById(R.id.btn_register)

        // Cargar intereses desde Firestore
        interestsRef.get().addOnCompleteListener { task: Task<QuerySnapshot> ->
            if (task.isSuccessful) {
                for (document in task.result) {
                    val groupName = document.id
                    val names = document["name"] as? List<String>
                    if (names != null) {
                        addGroupToLayout(groupName, names)
                    }
                }
            } else {
                Log.e("Firebase", "Error getting documents: ", task.exception)
            }
        }

        // Guardar selección al pulsar "Registrar"
        btnRegister.setOnClickListener {
            if (selectedTags.size < 10) {
                Toast.makeText(this, "Por favor, selecciona al menos 10 gustos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profileId = "profile_$uid"
            val perfilData = hashMapOf(
                "interests" to selectedTags.toList()
            )

            FirebaseFirestore.getInstance().collection("profiles").document(profileId)
                .set(perfilData, SetOptions.merge())

                .addOnSuccessListener {
                    Toast.makeText(this, "Intereses guardados correctamente", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AddphotosActivity::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar intereses", Toast.LENGTH_SHORT).show()
                }
        }

    }

    private fun addGroupToLayout(groupName: String, names: List<String>) {
        val llInterestVertical: LinearLayout = findViewById(R.id.ll_interest_vertical)
        val formattedGroupName = groupName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

        val groupTitle = TextView(this).apply {
            text = formattedGroupName
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 8)
        }
        llInterestVertical.addView(groupTitle)

        val chunked = names.chunked(3)
        val hiddenContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // Primera fila
        if (chunked.isNotEmpty()) {
            val firstRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val firstChunk = chunked[0]
            val visibleCount = if (chunked.size > 1) 3 else firstChunk.size
            for (i in 0 until visibleCount.coerceAtMost(firstChunk.size)) {
                firstRow.addView(createInterestTag(firstChunk[i]))
            }

            if (chunked.size > 1) {
                val arrow = ImageView(this).apply {
                    setImageResource(R.drawable.flecha)
                    layoutParams = LinearLayout.LayoutParams(36.dp, 24.dp).apply {
                        marginStart = 4.dp
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                arrow.setOnClickListener {
                    val visible = hiddenContainer.visibility == View.VISIBLE
                    hiddenContainer.visibility = if (visible) View.GONE else View.VISIBLE
                    arrow.rotation = if (visible) 0f else 180f
                }
                firstRow.addView(arrow)
            }

            if (chunked.size == 1 && firstChunk.size > visibleCount) {
                for (i in visibleCount until firstChunk.size) {
                    firstRow.addView(createInterestTag(firstChunk[i]))
                }
            }

            llInterestVertical.addView(firstRow)
        }

        if (chunked.size > 1) {
            for (i in 1 until chunked.size) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                chunked[i].forEach { tag ->
                    row.addView(createInterestTag(tag))
                }
                hiddenContainer.addView(row)
            }
            llInterestVertical.addView(hiddenContainer)
        }
    }

    private fun createInterestTag(text: String): TextView {
        val formattedText = text.replace("_", " ")
        return TextView(this).apply {
            this.text = formattedText
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(6, 6, 6, 6)
            setBackgroundResource(R.drawable.circle_background)
            layoutParams = LinearLayout.LayoutParams(92.dp, 44.dp).apply {
                marginEnd = 2.dp
            }
            gravity = Gravity.CENTER

            setOnClickListener {
                val tagText = formattedText
                if (selectedTags.contains(tagText)) {
                    selectedTags.remove(tagText)
                    setBackgroundResource(R.drawable.circle_background)
                } else {
                    selectedTags.add(tagText)
                    setBackgroundResource(R.drawable.circle_background_selected)
                }
            }
        }
    }

    val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
