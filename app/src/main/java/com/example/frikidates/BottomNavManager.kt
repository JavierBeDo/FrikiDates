package com.example.frikidates
import android.app.Activity
import android.content.Intent
import android.widget.ImageButton

object BottomNavManager {

    fun setupNavigation(activity: Activity, updateBeforeNav: (() -> Unit)? = null) {
        val navSearch = activity.findViewById<ImageButton>(R.id.nav_search)
        val navChat = activity.findViewById<ImageButton>(R.id.nav_chat)
        val navProfile = activity.findViewById<ImageButton>(R.id.nav_profile)

        navSearch.setOnClickListener {
            if (activity::class.java != MainMenuActivity::class.java) {
                updateBeforeNav?.invoke()
                activity.startActivity(Intent(activity, MainMenuActivity::class.java))
             //   activity.finish()
            }
        }

        navChat.setOnClickListener {
            if (activity::class.java != ChatsActivity::class.java) {
                updateBeforeNav?.invoke()
                activity.startActivity(Intent(activity, ChatsActivity::class.java))
                //   activity.finish()
            }
        }

        navProfile.setOnClickListener {
            if (activity::class.java != PerfilActivity::class.java) {
                updateBeforeNav?.invoke()
                activity.startActivity(Intent(activity, PerfilActivity::class.java))
                //   activity.finish()
            }
        }
    }
}

