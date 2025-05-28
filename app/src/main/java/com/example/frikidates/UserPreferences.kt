package com.example.frikidates

import android.content.Context

class UserPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(user: User) {
        with(sharedPreferences.edit()) {
            putString("userId", user.userId)
            //putString("imageUrl", user.imageUrl)
            apply()
        }
    }

    fun getUser(): User? {
        val userId = sharedPreferences.getString("userId", null)
        val imageUrl = sharedPreferences.getString("imageUrl", null)

        return if (userId != null) {
            User(userId)
        } else {
            null
        }
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

}
