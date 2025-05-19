// InterestManager.kt
package com.example.frikidates.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.frikidates.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class InterestManager(
    private val context: Context,
    private val container: LinearLayout,
    private val maxSelection: Int = 10
) {
    val selectedTags = mutableSetOf<String>()

    fun loadAndDisplayInterests(onLoaded: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null) {
        fetchUserInterests(
            onComplete = { userInterests ->
                selectedTags.addAll(userInterests)
                FirebaseFirestore.getInstance().collection("interests")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (document in snapshot) {
                            val groupName = document.id
                            val names = document["name"] as? List<String> ?: continue
                            addGroupToLayout(groupName, names)
                        }
                        onLoaded?.invoke()
                    }
                    .addOnFailureListener { e ->
                        onError?.invoke(e)
                    }
            },
            onError = { e -> onError?.invoke(e) }
        )
    }


    private fun addGroupToLayout(groupName: String, names: List<String>) {
        val formattedGroupName = groupName.replace("_", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

        val groupTitle = TextView(context).apply {
            text = formattedGroupName
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 8)
        }
        container.addView(groupTitle)

        val chunked = names.chunked(3)
        val hiddenContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // Primera fila
        if (chunked.isNotEmpty()) {
            val firstRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val firstChunk = chunked[0]
            val visibleCount = if (chunked.size > 1) 3 else firstChunk.size
            for (i in 0 until visibleCount.coerceAtMost(firstChunk.size)) {
                firstRow.addView(createInterestTag(firstChunk[i]))
            }

            if (chunked.size > 1) {
                val arrow = ImageView(context).apply {
                    setImageResource(R.drawable.flecha)
                    layoutParams = LinearLayout.LayoutParams(36.dp, 24.dp).apply {
                        marginStart = 4.dp
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setOnClickListener {
                        val visible = hiddenContainer.visibility == View.VISIBLE
                        hiddenContainer.visibility = if (visible) View.GONE else View.VISIBLE
                        rotation = if (visible) 0f else 180f
                    }
                }
                firstRow.addView(arrow)
            }

            container.addView(firstRow)
        }

        if (chunked.size > 1) {
            for (i in 1 until chunked.size) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                chunked[i].forEach { tag -> row.addView(createInterestTag(tag)) }
                hiddenContainer.addView(row)
            }
            container.addView(hiddenContainer)
        }
    }

    private fun addInterestToDatabase(interest: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileId = "profile_$uid"
        FirebaseFirestore.getInstance().collection("profiles").document(profileId)
            .update("interests", FieldValue.arrayUnion(interest.replace("_", " ")))
            .addOnSuccessListener {
                Log.d("Firestore", "Interés agregado correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al agregar interés", e)
            }
    }

    private fun removeInterestFromDatabase(interest: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileId = "profile_$uid"
        FirebaseFirestore.getInstance().collection("profiles").document(profileId)
            .update("interests", FieldValue.arrayRemove(interest.replace("_", " ")))
            .addOnSuccessListener {
                Log.d("Firestore", "Interés eliminado correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al eliminar interés", e)
            }
    }



    private fun createInterestTag(text: String): TextView {
        val formattedText = text.replace("_", " ")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val profileId = "profile_$uid"
        val db = FirebaseFirestore.getInstance()

        return TextView(context).apply {
            this.text = formattedText
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(6, 6, 6, 6)
            setBackgroundResource(R.drawable.circle_background)
            layoutParams = LinearLayout.LayoutParams(92.dp, 44.dp).apply {
                marginEnd = 2.dp
            }
            gravity = Gravity.CENTER

            if (selectedTags.contains(formattedText)) {
                setBackgroundResource(R.drawable.circle_background_selected)
            }

            setOnClickListener {
                if (selectedTags.contains(formattedText)) {
                    selectedTags.remove(formattedText)
                    setBackgroundResource(R.drawable.circle_background)

                    removeInterestFromDatabase(formattedText)

                } else {
                    if (selectedTags.size >= maxSelection) {
                        Toast.makeText(context, "Máximo $maxSelection intereses", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    selectedTags.add(formattedText)
                    setBackgroundResource(R.drawable.circle_background_selected)

                    // ✅ Añadir a Firestore
                    addInterestToDatabase(formattedText)
                }
            }
        }
    }


    private fun fetchUserInterests(onComplete: (Set<String>) -> Unit, onError: (Exception) -> Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val profileId = "profile_$uid"
        FirebaseFirestore.getInstance()
            .collection("profiles")
            .document(profileId)
            .get()
            .addOnSuccessListener { document ->
                val interests = document.get("interests") as? List<String> ?: emptyList()
                onComplete(interests.map { it.replace("_", " ") }.toSet())
            }
            .addOnFailureListener { e -> onError(e) }
    }


    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
