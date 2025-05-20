package com.example.frikidates

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
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

//    private fun getDummyData(): List<Profile> {
//        return listOf(
//            Profile("María", 26, "Madrid", "Alta compatibilidad", listOf(R.drawable.user1, R.drawable.user1_2)),
//            Profile("Laura", 24, "Barcelona", "Muy compatible", listOf(R.drawable.user2, R.drawable.user2_2, R.drawable.user2_3)),
//            Profile("Lucía", 28, "Sevilla", "Compatible", listOf(R.drawable.user3))
//        )
//    }

    private fun loadProfilesFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUser == null) {
            Log.w("Firestore", "Usuario actual no logueado, no se pueden cargar perfiles.")
            // Aquí podrías limpiar el adaptador o mostrar un mensaje al usuario
            cardStackView.adapter = CardStackAdapter(this, emptyList()) // Ejemplo de limpiar
            return
        }

        db.collection("profiles")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("Firestore", "No se encontraron perfiles.")
                    cardStackView.adapter = CardStackAdapter(this@MainMenuActivity, emptyList())
                    return@addOnSuccessListener
                }
                val profilesList = mutableListOf<Profile>()
                val potentialMatchesCount =
                    result.documents.count() { it.getString("email") != currentUserEmail }
                if (potentialMatchesCount == 0) {
                    Log.d("Firestore", "No hay otros perfiles para mostrar.")
                    cardStackView.adapter = CardStackAdapter(this@MainMenuActivity, emptyList())
                    return@addOnSuccessListener
                }
                var processedPotentialMatches = 0

                for (document in result) {
                    val profileDocumentId = document.id
                    val data = document.data

                    val email = data["email"] as? String ?: continue
                    if (email == currentUserEmail) {
                        continue
                    }

                    val name = data["name"] as? String ?: ""
                    val age = 23 //TODO obtener edad de firebase
                    val gender = data["genero"] as? String ?: "Género desconocido"
                    val city = data["city"] as? String ?: "Ubicación desconocida"
                    val compatibility = "Compatibilidad desconocida"

                    //val interests = (data["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    loadImageUrlsFromStorage(profileDocumentId) { imageUrls ->
                        val profile = Profile(
                            name = name,
                            age = age,
                            gender = gender,
                            city = city,
                            compatibility = compatibility,
                            images = imageUrls
                        )
                        profilesList.add(profile)

                        processedPotentialMatches++
                        if (processedPotentialMatches == potentialMatchesCount) {
                            // << CAMBIO AQUÍ: Pasar 'this@MainMenuActivity' para el Context
                            cardStackView.adapter =
                                CardStackAdapter(this@MainMenuActivity, profilesList)
                            Log.d(
                                "Firestore",
                                "Adaptador actualizado con ${profilesList.size} perfiles."
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al cargar perfiles", exception)
                // Opcionalmente, mostrar un mensaje de error o un estado vacío
                cardStackView.adapter = CardStackAdapter(this@MainMenuActivity, emptyList())
            }
    }

    private fun loadImageUrlsFromStorage(profileId: String, callback: (List<String>) -> Unit) {
        Log.i(
            "StorageLoad",
            "Attempting to load images for profileId: '$profileId'"
        ) // Log el ID exacto

        if (profileId.isBlank()) { // Usar isBlank() para cubrir null, empty y solo espacios en blanco
            Log.e("StorageLoad", "profileId is blank. Cannot create storage reference.")
            callback(emptyList())
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference.child(profileId)
        Log.d(
            "StorageLoad",
            "Storage reference path: ${storageRef.path}"
        ) // Verifica la ruta completa

        storageRef.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                Log.i(
                    "StorageLoad",
                    "listAll SUCCESS for '$profileId'. Items found: ${listResult.items.size}, Prefixes: ${listResult.prefixes.size}"
                )
                if (listResult.items.isEmpty()) {
                    Log.w("StorageLoad", "No items (images) found in folder '$profileId'")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val imageUrls = mutableListOf<String>()
                val totalItems = listResult.items.size
                var loadedCount = 0

                listResult.items.forEach { item ->
                    Log.d("StorageLoad", "Processing item: ${item.name} in folder '$profileId'")
                    item.downloadUrl.addOnSuccessListener { uri ->
                        Log.i("StorageLoad", "SUCCESS getting download URL for ${item.name}: $uri")
                        imageUrls.add(uri.toString())
                        loadedCount++
                        if (loadedCount == totalItems) {
                            Log.i(
                                "StorageLoad",
                                "All ${totalItems} URLs loaded for '$profileId'. Passing to callback: $imageUrls"
                            )
                            callback(imageUrls)
                        }
                    }.addOnFailureListener { exception ->
                        Log.e(
                            "StorageLoad",
                            "FAILURE getting download URL for ${item.name} in '$profileId'",
                            exception
                        )
                        loadedCount++ // Incrementar incluso en fallo para que el callback final se llame
                        if (loadedCount == totalItems) {
                            Log.w(
                                "StorageLoad",
                                "Finished processing all items for '$profileId' but some URLs failed. Passing to callback: $imageUrls"
                            )
                            callback(imageUrls) // Pasar las que se hayan podido cargar
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("StorageLoad", "listAll FAILURE for profileId: '$profileId'", exception)
                callback(emptyList())
            }
    }
}
