package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.firebase.FirebaseRepository
import com.example.frikidates.utils.InterestManager

class LoginInterestsActivity : AppCompatActivity() {

    private lateinit var interestManager: InterestManager
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_interests)

        val layout = findViewById<LinearLayout>(R.id.ll_interest_vertical)
        btnRegister = findViewById(R.id.btn_register)

        interestManager = InterestManager(this, layout)

        interestManager.loadAndDisplayInterests(
            onError = {
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_interests),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        btnRegister.setOnClickListener {
            if (interestManager.selectedTags.size < 10) {
                Toast.makeText(
                    this,
                    getString(R.string.select_at_least_10_interests),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            FirebaseRepository.saveUserInterests(
                interests = interestManager.selectedTags.toList(),
                onSuccess = {
                    Toast.makeText(this, getString(R.string.interests_saved), Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this, AddphotosActivity::class.java))
                },
                onFailure = {
                    Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT)
                        .show()
                }
            )
        }
    }
}

