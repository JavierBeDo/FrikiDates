package com.example.frikidates

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.frikidates.firebase.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting


class MainMenuActivity : AppCompatActivity() {

    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager

    private lateinit var btnLike: ImageView
    private lateinit var btnDislike: ImageView
    private lateinit var btnRewind: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // CardStack
        cardStackView = findViewById(R.id.card_stack_view)
        manager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {}
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: android.view.View?, position: Int) {}
            override fun onCardDisappeared(view: android.view.View?, position: Int) {}
        })
        cardStackView.layoutManager = manager
        loadProfilesFromFirestore()
        BottomNavManager.setupNavigation(this)

        // Botones de acción
        btnLike = findViewById(R.id.btn_like)
        btnDislike = findViewById(R.id.btn_dislike)
        btnRewind = findViewById(R.id.btn_rewind)


        btnLike.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(200)
                .build()
            manager.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }

        btnDislike.setOnClickListener {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(200)
                .build()
            manager.setSwipeAnimationSetting(setting)
            cardStackView.swipe()
        }

        btnRewind.setOnClickListener {
            val setting = RewindAnimationSetting.Builder()
                .setDirection(Direction.Bottom)
                .setDuration(200)
                .build()
            manager.setRewindAnimationSetting(setting)
            cardStackView.rewind()
        }


    }

    private fun loadProfilesFromFirestore() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        FirebaseRepository.loadProfiles(
            currentUserEmail,
            onSuccess = { profiles ->
                cardStackView.adapter = CardStackAdapter(profiles, this)

                profiles.forEach { profile ->
                    FirebaseRepository.loadImageUrlsFromStorageMain(
                        profile.profileId,
                        onSuccess = { urls ->
                            Log.d(
                                "MainMenuActivity",
                                getString(R.string.images_loaded_for, profile.name, urls.toString())
                            )
                        },
                        onFailure = { exception ->
                            Log.e(
                                "MainMenuActivity",
                                getString(R.string.error_loading_images_for, profile.name),
                                exception
                            )
                        }
                    )
                }
            },
            onError = {
                cardStackView.adapter = CardStackAdapter(emptyList(), this)
                Log.e("MainMenuActivity", getString(R.string.error_loading_profiles), it)
            }
        )
    }


}
