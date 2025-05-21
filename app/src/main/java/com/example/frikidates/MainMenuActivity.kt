package com.example.frikidates

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.vector.path
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

    private lateinit var nav_profile: ImageView
    private lateinit var nav_chat: ImageView
    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager

    private lateinit var btnLike: ImageView
    private lateinit var btnDislike: ImageView
    private lateinit var btnRewind: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Botones navegación inferior
        nav_profile = findViewById(R.id.nav_profile)
        nav_chat = findViewById(R.id.nav_chat)

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

        // Botones de acción
        btnLike = findViewById(R.id.btn_like)
        btnDislike = findViewById(R.id.btn_dislike)
        btnRewind = findViewById(R.id.btn_rewind)

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

                    loadAllProfileImages(profileDocumentId) { imageUrls ->
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

}
