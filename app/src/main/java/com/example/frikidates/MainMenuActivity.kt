package com.example.frikidates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frikidates.firebase.FirebaseRepository
import com.example.frikidates.util.LocationEncryptionHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.*

class MainMenuActivity : AppCompatActivity() {

    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager
    private lateinit var btnLike: ImageView
    private lateinit var btnDislike: ImageView
    private lateinit var btnRewind: ImageView
    private lateinit var locationPermissionRequest: androidx.activity.result.ActivityResultLauncher<String>

    private val profilesList = mutableListOf<Profile>()
    private var currentUserInterests = listOf<String>()
    private var currentUserLatitude: Double = 0.0
    private var currentUserLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        btnLike = findViewById(R.id.btn_like)
        btnDislike = findViewById(R.id.btn_dislike)
        btnRewind = findViewById(R.id.btn_rewind)
        cardStackView = findViewById(R.id.card_stack_view)

        // Initialize CardStackView
        manager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {
                Log.d("CardStack", "Swiped: $direction, profiles left: ${profilesList.size - manager.topPosition}")
            }
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {
                Log.d("CardStack", "Rewound")
            }
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: android.view.View?, position: Int) {
                Log.d("CardStack", "Card appeared at position: $position")
            }
            override fun onCardDisappeared(view: android.view.View?, position: Int) {
                Log.d("CardStack", "Card disappeared at position: $position")
            }
        })
        cardStackView.layoutManager = manager

        BottomNavManager.setupNavigation(this)

        // Button click listeners
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

        // Location permission request
        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Necesitamos tu ubicación para mostrarte personas cercanas", Toast.LENGTH_LONG).show()
                loadCurrentUserInterestsAndProfiles()
            }
        }

        solicitarPermisoUbicacion()
    }

    private fun loadCurrentUserInterestsAndProfiles() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("MainMenuActivity", "Usuario no autenticado")
            cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            return
        }

        FirebaseRepository.fetchUserInterests(
            onSuccess = { interests ->
                currentUserInterests = interests
                Log.d("MainMenuActivity", "User interests: $currentUserInterests")
                loadProfilesFromFirestore()
            },
            onError = { e ->
                Log.e("MainMenuActivity", "Error fetching user interests", e)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            }
        )
    }

    private fun loadProfilesFromFirestore() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        FirebaseRepository.loadProfiles(
            currentUserEmail,
            onSuccess = { profiles ->
                profilesList.clear()
                profilesList.addAll(profiles)
                cardStackView.adapter = CardStackAdapter(
                    this,
                    profilesList,
                    currentUserInterests,
                    currentUserLatitude,
                    currentUserLongitude
                )
                Log.d("MainMenuActivity", "Adapter updated with ${profilesList.size} profiles")
            },
            onError = { e ->
                Log.e("MainMenuActivity", getString(R.string.error_loading_profiles), e)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            }
        )
    }

    private fun solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        Log.w("Location", "Location is null")
                        loadCurrentUserInterestsAndProfiles()
                        return@addOnSuccessListener
                    }

                    currentUserLatitude = location.latitude
                    currentUserLongitude = location.longitude
                    val encryptedLocation = LocationEncryptionHelper.encryptLocation(currentUserLatitude, currentUserLongitude)
                    saveLocationToFirebase(encryptedLocation, currentUserLatitude, currentUserLongitude)
                    loadCurrentUserInterestsAndProfiles()
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error getting location", e)
                    loadCurrentUserInterestsAndProfiles()
                }
        } else {
            Log.w("Location", "Permission not granted")
            loadCurrentUserInterestsAndProfiles()
        }
    }

    private fun saveLocationToFirebase(encryptedLocation: String, latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseRepository.saveUserLocation(
            userId,
            encryptedLocation,
            latitude,
            longitude,
            onSuccess = { Log.d("LocationUpdate", "Location saved") },
            onFailure = { e -> Log.e("LocationUpdate", "Error saving location", e) }
        )
    }
}