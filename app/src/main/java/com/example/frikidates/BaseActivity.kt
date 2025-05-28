package com.example.frikidates

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.firebase.FirebaseRepository
import com.google.android.play.integrity.internal.c
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

open class BaseActivity : AppCompatActivity() {

    open class BaseActivity : AppCompatActivity() {

        private var connectionListener: ValueEventListener? = null

        override fun onStart() {
            super.onStart()
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val userId = user.uid.removePrefix("profile_")

            connectionListener = FirebaseRepository.observeUserConnectionStatus(this, userId) { status ->
                Log.d("BaseActivity", getString(R.string.connection_status_changed, status))
            }
        }

        override fun onStop() {
            super.onStop()
            connectionListener?.let { FirebaseRepository.removeConnectionListener(it) }
        }
    }

}
