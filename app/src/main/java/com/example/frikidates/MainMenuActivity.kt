package com.example.frikidates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frikidates.util.LocationEncryptionHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting

class MainMenuActivity : AppCompatActivity() {

    private lateinit var nav_profile: ImageView
    private lateinit var nav_chat: ImageView
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

        // Botones navegación inferior
        nav_profile = findViewById(R.id.nav_profile)
        nav_chat = findViewById(R.id.nav_chat)

        // Botones de acción
        btnLike = findViewById(R.id.btn_like)
        btnDislike = findViewById(R.id.btn_dislike)
        btnRewind = findViewById(R.id.btn_rewind)

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getUserLocation()
            } else {
                Toast.makeText(
                    this,
                    "Necesitamos tu ubicación para mostrarte personas cercanas",
                    Toast.LENGTH_LONG
                ).show()
                // Cargar perfiles sin ubicación
                loadCurrentUserInterestsAndProfiles()
            }
        }

        // Lanza la petición
        solicitarPermisoUbicacion()

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

        // Acciones navegación
        nav_chat.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
        }

        nav_profile.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

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

    private fun loadCurrentUserInterestsAndProfiles() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("Firestore", "Usuario actual no logueado, no se pueden cargar perfiles.")
            cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            return
        }

        // Obtener intereses del usuario actual
        db.collection("profiles").document("profile_${currentUser.uid}")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserInterests = (document.get("interests") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    loadProfilesFromFirestore()
                } else {
                    Log.w("Firestore", "Perfil del usuario actual no encontrado.")
                    cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al obtener intereses del usuario actual", exception)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            }
    }

    private fun loadProfilesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (currentUser == null) {
            Log.w("Firestore", "Usuario actual no logueado, no se pueden cargar perfiles.")
            cardStackView.adapter = CardStackAdapter(this, emptyList(), emptyList(), 0.0, 0.0)
            return
        }

        db.collection("profiles")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("Firestore", "No se encontraron perfiles.")
                    cardStackView.adapter = CardStackAdapter(this, emptyList(), currentUserInterests, currentUserLatitude, currentUserLongitude)
                    return@addOnSuccessListener
                }

                val profilesToProcess = result.documents.filter { it.getString("email") != currentUserEmail }
                val potentialMatchesCount = profilesToProcess.size
                if (potentialMatchesCount == 0) {
                    Log.d("Firestore", "No hay otros perfiles para mostrar.")
                    cardStackView.adapter = CardStackAdapter(this, emptyList(), currentUserInterests, currentUserLatitude, currentUserLongitude)
                    return@addOnSuccessListener
                }
                var processedPotentialMatches = 0

                for (document in profilesToProcess) {
                    val profileDocumentId = document.id
                    val data = document.data ?: continue

                    val email = data["email"] as? String ?: continue
                    if (email == currentUserEmail) continue

                    val name = data["name"] as? String ?: ""
                    val birthdate = data["birthdate"] as? String ?: ""
                    val gender = data["genero"] as? String ?: "Género desconocido"
                    val encryptedLocation = data["encryptedLocation"] as? String ?: ""
                    val interests = (data["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    loadAllProfileImages(profileDocumentId) { imageUrls ->
                        val profile = Profile(
                            id = profileDocumentId,
                            name = name,
                            birthdate = birthdate,
                            gender = gender,
                            encryptedLocation = encryptedLocation,
                            interests = interests,
                            images = imageUrls
                        )
                        profilesList.add(profile)
                        processedPotentialMatches++

                        if (processedPotentialMatches == potentialMatchesCount) {
                            cardStackView.adapter = CardStackAdapter(
                                this@MainMenuActivity,
                                profilesList,
                                currentUserInterests,
                                currentUserLatitude,
                                currentUserLongitude
                            )
                            Log.d("Firestore", "Adaptador actualizado con ${profilesList.size} perfiles.")
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al cargar perfiles", exception)
                cardStackView.adapter = CardStackAdapter(this, emptyList(), currentUserInterests, currentUserLatitude, currentUserLongitude)
            }
    }

    private fun loadAllProfileImages(profileId: String, onResult: (List<String>) -> Unit) {
        if (profileId.isBlank()) {
            Log.e("StorageLoad", "profileId is blank, no se puede obtener imágenes.")
            onResult(emptyList())
            return
        }

        val folderName = profileId.removePrefix("profile_")
        val storageRef = FirebaseStorage.getInstance().reference.child(folderName)

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    Log.w("StorageLoad", "No hay imágenes para $folderName")
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                val urls = mutableListOf<String>()
                val total = listResult.items.size
                var loaded = 0

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
                            Log.e("StorageLoad", "Error al obtener URL de ${item.name}", it)
                            loaded++
                            if (loaded == total) {
                                onResult(urls)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("StorageLoad", "Error al listar imágenes para $folderName", it)
                onResult(emptyList())
            }
    }

    private fun solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        Log.w("Location", "Ubicación es null, no se pudo obtener la ubicación actual.")
                        Toast.makeText(
                            this,
                            "No se pudo obtener tu ubicación. Intenta más tarde.",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCurrentUserInterestsAndProfiles()
                        return@addOnSuccessListener
                    }

                    currentUserLatitude = location.latitude
                    currentUserLongitude = location.longitude
                    val encryptedLocation = LocationEncryptionHelper.encryptLocation(currentUserLatitude, currentUserLongitude)

                    if (currentUserLatitude == 0.0 && currentUserLongitude == 0.0) {
                        Log.w("Location", "Ubicación descifrada inválida: lat y lon son 0.0")
                        Toast.makeText(
                            this,
                            "Ubicación inválida detectada. Intenta reiniciar la app.",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadCurrentUserInterestsAndProfiles()
                        return@addOnSuccessListener
                    }

                    saveToFirebase(encryptedLocation, currentUserLatitude, currentUserLongitude)
                    loadCurrentUserInterestsAndProfiles()
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Error al obtener la ubicación", e)
                    Toast.makeText(
                        this,
                        "Error al obtener ubicación: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCurrentUserInterestsAndProfiles()
                }
        } else {
            Log.w("Location", "Permiso de ubicación no concedido")
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
            loadCurrentUserInterestsAndProfiles()
        }
    }

    private fun saveToFirebase(encryptedLocation: String, newLat: Double, newLon: Double) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val profileRef = db.collection("profiles").document("profile_$userId")
        val locationCollectionRef = profileRef.collection("location")
        val lastLocationDocRef = locationCollectionRef.document("actual")

        if (!LocationEncryptionHelper.isKeyValid()) {
            Log.w("Ubicación", "Clave eliminada. Limpiando datos de ubicación antigua.")
            lastLocationDocRef.delete()
                .addOnSuccessListener {
                    Log.d("Ubicación", "Ubicación anterior eliminada correctamente.")
                }
                .addOnFailureListener {
                    Log.e("Ubicación", "Error al eliminar la ubicación anterior", it)
                }
            return
        }

        lastLocationDocRef.get().addOnSuccessListener { document ->
            val lastTimestamp = document.getTimestamp("timestamp")?.toDate()
            val lastEncryptedLocation = document.getString("encryptedLocation")

            val now = System.currentTimeMillis()
            val shouldUpdateTime =
                lastTimestamp == null || now - lastTimestamp.time > 5 * 60 * 1000 // 5 minutos

            val shouldUpdateLocation = if (lastEncryptedLocation == null) {
                true
            } else {
                LocationEncryptionHelper.hasLocationChangedSignificantly(
                    lastEncryptedLocation,
                    newLat,
                    newLon
                )
            }

            if (shouldUpdateTime || shouldUpdateLocation) {
                val updateData = mapOf(
                    "encryptedLocation" to encryptedLocation,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                lastLocationDocRef.set(updateData)
                    .addOnSuccessListener {
                        Log.d("LocationUpdate", "Ubicación encriptada guardada correctamente en subcolección.")
                    }
                    .addOnFailureListener {
                        Log.e("LocationUpdate", "Error al guardar ubicación encriptada en subcolección", it)
                    }
            } else {
                Log.d("LocationUpdate", "Ubicación no actualizada: sin cambios relevantes.")
            }
        }.addOnFailureListener {
            Log.e("LocationUpdate", "Error al obtener documento de ubicación para comprobar ubicación", it)
        }
    }
}