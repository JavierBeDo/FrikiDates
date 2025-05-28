package com.example.frikidates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frikidates.util.LocationEncryptionHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>

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

        // CardStack
        cardStackView = findViewById(R.id.card_stack_view)

        manager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {
                Log.d("CardStack", "Swiped: $direction, profiles left: ${profilesList.size - manager.topPosition}")
            }
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {
                Log.d("CardStack", "Rewound")
            }
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: View?, position: Int) {
                Log.d("CardStack", "Card appeared at position: $position")
            }
            override fun onCardDisappeared(view: View?, position: Int) {
                Log.d("CardStack", "Card disappeared at position: $position")
            }
        })
        cardStackView.layoutManager = manager

        BottomNavManager.setupNavigation(this)

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
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("Firestore", "Usuario no logueado")
            cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            return
        }

        db.collection("profiles").document("profile_${currentUser.uid}")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserInterests = (document.get("interests") as? List<*>)?.filterIsInstance<String>()?.filterNotNull() ?: emptyList()
                    Log.d("Firestore", "User interests: $currentUserInterests")
                    loadProfilesFromFirestore()
                } else {
                    Log.w("Firestore", "Perfil no encontrado")
                    cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener intereses", e)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
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
                            profilesList.add(profile)
                            processedCount++
                            if (processedCount == profilesToProcess.size) {
                                cardStackView.adapter = CardStackAdapter(
                                    this@MainMenuActivity,
                                    profilesList,
                                    currentUserInterests,
                                    currentUserLatitude,
                                    currentUserLongitude
                                )
                                Log.d("Firestore", "Adapter updated with ${profilesList.size} profiles")
                            }
                        }
                    )
                }
            },
            onError = {
                cardStackView.adapter = CardStackAdapter(emptyList(), this)
                Log.e("MainMenuActivity", getString(R.string.error_loading_profiles), it)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al cargar perfiles", e)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), currentUserInterests, currentUserLatitude, currentUserLongitude)
            }
    }

    private fun loadAllProfileImages(profileId: String, onResult: (List<String>) -> Unit) {
        if (profileId.isBlank()) {
            Log.e("StorageLoad", "profileId is blank")
            onResult(emptyList())
            return
        }

        val folderName = profileId.removePrefix("profile_")
        val storageRef = FirebaseStorage.getInstance().reference.child(folderName)

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    Log.w("StorageLoad", "No images for $folderName")
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val urls = mutableListOf<String>()
                var loaded = 0
                val total = listResult.items.size

                for (item in listResult.items) {
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            urls.add(uri.toString())
                            loaded++
                            if (loaded == total) {
                                onResult(urls)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("StorageLoad", "Error getting URL for ${item.name}", it)
                            loaded++
                            if (loaded == total) {
                                onResult(urls)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("StorageLoad", "Error listing images for $folderName", it)
                onResult(emptyList())
            }
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
                    saveToFirebase(encryptedLocation, currentUserLatitude, currentUserLongitude)
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

    private fun saveToFirebase(encryptedLocation: String, newLat: Double, newLon: Double) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileRef = db.collection("profiles").document("profile_$userId")
        val locationCollectionRef = profileRef.collection("location")
        val lastLocationDocRef = locationCollectionRef.document("actual")

        lastLocationDocRef.get().addOnSuccessListener { document ->
            val lastTimestamp = document.getTimestamp("timestamp")?.toDate()
            val lastEncryptedLocation = document.getString("encryptedLocation")

            val now = System.currentTimeMillis()
            val shouldUpdateTime = lastTimestamp == null || now - lastTimestamp.time > 5 * 60 * 1000
            val shouldUpdateLocation = lastEncryptedLocation == null || LocationEncryptionHelper.hasLocationChangedSignificantly(
                lastEncryptedLocation,
                newLat,
                newLon
            )

            if (shouldUpdateTime || shouldUpdateLocation) {
                val updateData = mapOf(
                    "encryptedLocation" to encryptedLocation,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                lastLocationDocRef.set(updateData)
                    .addOnSuccessListener {
                        Log.d("LocationUpdate", "Location saved")
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocationUpdate", "Error saving location", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("LocationUpdate", "Error checking location", e)
        }
    }
}