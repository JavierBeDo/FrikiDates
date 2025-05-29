// InterestManager.kt
package com.example.frikidates.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.frikidates.R
import com.example.frikidates.firebase.FirebaseRepository
class InterestManager(
    private val context: Context,
    private val container: LinearLayout
) {
    val selectedTags = mutableSetOf<String>()
    private val maxSelection = 10

    fun loadAndDisplayInterests(
        onLoaded: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        FirebaseRepository.fetchUserInterests(
            onSuccess = { userInterests ->
                selectedTags.clear()
                selectedTags.addAll(userInterests) // <-- Aquí se cargan los intereses del usuario

                FirebaseRepository.fetchAllInterests(
                    onSuccess = { groups ->
                        container.removeAllViews()
                        for ((groupName, names) in groups) {
                            addGroupToLayout(groupName, names)
                        }
                        onLoaded?.invoke()
                    },
                    onError = { e -> onError?.invoke(e) }
                )
            },
            onError = { e -> onError?.invoke(e) }
        )
    }


    private fun addGroupToLayout(groupName: String, names: List<String>) {
        val formattedGroupName = groupName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

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
                    setImageResource(R.drawable.downarrow)
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

    private fun createInterestTag(text: String): TextView {
        val formattedText = text.replace("_", " ")
        return TextView(context).apply {
            this.text = formattedText
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(6, 6, 6, 6)
            setBackgroundResource(if (selectedTags.contains(formattedText)) R.drawable.circle_background_selected else R.drawable.circle_background)
            layoutParams = LinearLayout.LayoutParams(92.dp, 44.dp).apply {
                marginEnd = 2.dp
                bottomMargin = 2.dp
            }
            gravity = Gravity.CENTER

            setOnClickListener {
                if (selectedTags.contains(formattedText)) {
                    selectedTags.remove(formattedText)
                    setBackgroundResource(R.drawable.circle_background)
                } else {
                    if (selectedTags.size >= maxSelection) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.max_interests_toast, maxSelection),
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }
                    selectedTags.add(formattedText)
                    setBackgroundResource(R.drawable.circle_background_selected)
                }
            }
        }
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
