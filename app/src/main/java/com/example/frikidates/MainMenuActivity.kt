package com.example.frikidates

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.frikidates.firebase.FirebaseRepository
import com.example.frikidates.util.LocationEncryptionHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.DocumentSnapshot
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
    private var rewindCount = 0
    private val maxRewinds = 1

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
                val position = manager.topPosition - 1
                if (position < 0 || position >= profilesList.size) {
                    Log.w("CardStack", "Posición inválida: $position, profilesList.size=${profilesList.size}")
                    return
                }
                val swipedProfile = profilesList[position]
                val userId = FirebaseRepository.getCurrentUserId() ?: run {
                    Log.e("CardStack", "Usuario no autenticado")
                    Toast.makeText(this@MainMenuActivity, "Error: usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return
                }
                when (direction) {
                    Direction.Right -> {
                        FirebaseRepository.registerSwipe(
                            userId,
                            swipedProfile.id,
                            "like",
                            onSuccess = {
                                Log.d("CardStack", "Swipe like registrado para ${swipedProfile.id}")
                                FirebaseRepository.checkForMatch(
                                    userId,
                                    swipedProfile.id,
                                    onMatch = { matchId ->
                                        FirebaseRepository.createMatch(
                                            userId,
                                            swipedProfile.id,
                                            onSuccess = {
                                                Log.d("CardStack", "Match creado: $matchId")
                                                Toast.makeText(this@MainMenuActivity, "¡Match con ${swipedProfile.name}!", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { e ->
                                                Log.e("CardStack", "Error creando match: ${e.message}", e)
                                                Toast.makeText(this@MainMenuActivity, "Error creando match", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onNoMatch = { Log.d("CardStack", "No match con ${swipedProfile.id}") },
                                    onError = { e ->
                                        Log.e("CardStack", "Error verificando match: ${e.message}", e)
                                        Toast.makeText(this@MainMenuActivity, "Error verificando match", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onFailure = { e ->
                                Log.e("CardStack", "Error registrando swipe: ${e.message}", e)
                                Toast.makeText(this@MainMenuActivity, "Error registrando swipe", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Direction.Left -> {
                        Log.d("CardStack", "Dislike en ${swipedProfile.id}, no se registra")
                    }
                    else -> return
                }
                rewindCount = 0
                btnRewind.isEnabled = true
                btnRewind.alpha = 1.0f
                Log.d("CardStack", "Profiles left: ${profilesList.size - manager.topPosition}")

                if (manager.topPosition >= profilesList.size - 5) {
                    loadInterestsAndProfiles()
                }
            }
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {
                Log.d("CardStack", "Rewound, count: $rewindCount")
            }
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: android.view.View?, position: Int) {
                Log.d("CardStack", "Card appeared at position: $position")
            }
            override fun onCardDisappeared(view: android.view.View?, position: Int) {
                Log.d("CardStack", "Card disappeared at position: $position")
            }
        }).apply {
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
        }
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
            if (rewindCount < maxRewinds) {
                val setting = RewindAnimationSetting.Builder()
                    .setDirection(Direction.Bottom)
                    .setDuration(200)
                    .build()
                manager.setRewindAnimationSetting(setting)
                cardStackView.rewind()
                rewindCount++
                Log.d("Rewind", "Rewind $rewindCount of $maxRewinds")
                if (rewindCount >= maxRewinds) {
                    btnRewind.isEnabled = false
                    btnRewind.alpha = 0.5f
                    Toast.makeText(this, "Límite de retrocesos alcanzado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Límite de retrocesos alcanzado", Toast.LENGTH_SHORT).show()
            }
        }

        // Location permission request
        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Necesitamos tu ubicación para mostrarte personas cercanas", Toast.LENGTH_LONG).show()
                loadInterestsAndProfiles()
            }
        }

        solicitarPermisoUbicacion()
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        Log.w("Location", "Location is null")
                        loadInterestsAndProfiles()
                        return@addOnSuccessListener
                    }
                    currentUserLatitude = location.latitude
                    currentUserLongitude = location.longitude
                    val encryptedLocation = LocationEncryptionHelper.encryptLocation(currentUserLatitude, currentUserLongitude)
                    val userId = FirebaseRepository.getCurrentUserId()
                    if (userId == null) {
                        Log.e("Location", "Usuario no autenticado")
                        loadInterestsAndProfiles()
                        return@addOnSuccessListener
                    }
                    FirebaseRepository.saveUserLocation(
                        userId,
                        encryptedLocation,
                        currentUserLatitude,
                        currentUserLongitude,
                        onSuccess = { Log.d("LocationUpdate", "Location saved") },
                        onFailure = { e ->
                            Log.e("LocationUpdate", "Error saving location", e)
                            loadInterestsAndProfiles()
                        }
                    )
                    loadInterestsAndProfiles()
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error getting location", e)
                    loadInterestsAndProfiles()
                }
        } else {
            Log.w("Location", "Permission not granted")
            loadInterestsAndProfiles()
        }
    }

    private fun loadInterestsAndProfiles() {
        FirebaseRepository.loadUserInterestsAndProfiles(
            currentUserLatitude,
            currentUserLongitude,
            onSuccess = { interests, profiles ->
                currentUserInterests = interests
                profilesList.clear()
                profilesList.addAll(profiles)
                cardStackView.adapter = CardStackAdapter(
                    this,
                    profilesList,
                    currentUserInterests,
                    currentUserLatitude,
                    currentUserLongitude
                )
                Log.d("MainMenuActivity", "Adapter updated with ${profilesList.size} profiles, interests: $interests")
            },
            onError = { e ->
                Log.e("MainMenuActivity", "Error loading interests or profiles: ${e.message}", e)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            }
        )
    }
}