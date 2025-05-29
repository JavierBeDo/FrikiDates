package com.example.frikidates.firebase

import MensajeEnviar
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.frikidates.HolderChats
import com.example.frikidates.MensajeRecibir
import com.example.frikidates.Profile
import com.example.frikidates.R
import com.example.frikidates.util.LocationEncryptionHelper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object FirebaseRepository {

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val rtdbInstance = FirebaseDatabase.getInstance("https://frikidatesdb-default-rtdb.europe-west1.firebasedatabase.app")
    private val connectedRef = rtdbInstance.getReference(".info/connected")

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext // evitar fugas de memoria
    }


    fun loginUser(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    fun registerUser(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

//    fun getUserDocument(uid: String) =
//        db.collection("users").document(uid).get()

    fun getProfileDocument(profileId: String) =
        db.collection("profiles").document(profileId).get()

    fun saveUserDocument(uid: String, userData: Map<String, Any>): Task<Void> {
        return db.collection("user").document(uid).set(userData)
    }

    fun saveProfileDocument(profileId: String, profileData: Map<String, Any>): Task<Void> {
        return db.collection("profiles").document(profileId).set(profileData)
    }

    /**
     * Combina creación del perfil + usuario en Firestore.
     */
    fun createUserProfile(
        uid: String,
        name: String,
        surname: String,
        email: String,
        birthdate: String,
        gender: String,
        genderPref: String,
        ageRangeMin: Int,
        ageRangeMax: Int,
        desc: String,
        distanciaMax: Int
    ): Task<Void> {
        val profileId = "profile_$uid"

        val profileData = mapOf(
            "name" to name,
            "surname" to surname,
            "email" to email,
            "birthdate" to birthdate,
            "genero" to gender,
            "preferenciaGenero" to genderPref,
            "rangoEdadMin" to ageRangeMin,
            "rangoEdadMax" to ageRangeMax,
            "distanciaMax" to distanciaMax,
            "bio" to desc,
            "notificaciones" to true
        )

        val userData = mapOf(
            "status" to "active",
            "profileId" to profileId
        )

        // Guarda primero perfil, luego usuario
        return saveProfileDocument(profileId, profileData)
            .continueWithTask { saveUserDocument(uid, userData) }
    }

    fun loginAndLoadProfile(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        loginUser(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onError(appContext.getString(R.string.error_generic))
                    return@addOnSuccessListener
                }

                db.collection("user").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val profileId = userDoc.getString("profileId")

                        if (!profileId.isNullOrEmpty()) {
                            db.collection("profiles").document(profileId).get()
                                .addOnSuccessListener { profileDoc ->
                                    if (profileDoc.exists()) {
                                        onSuccess(uid)
                                    } else {
                                        onError(appContext.getString(R.string.error_obtaining_profile, "Perfil no encontrado"))
                                    }
                                }
                                .addOnFailureListener { e ->
                                    onError(appContext.getString(R.string.error_obtaining_profile, e.message ?: ""))
                                }
                        } else {
                            onError(appContext.getString(R.string.error_obtaining_profile))
                        }
                    }
                    .addOnFailureListener { e ->
                        onError(appContext.getString(R.string.error_obtaining_user, e.message ?: ""))
                    }
            }
            .addOnFailureListener { e ->
                onError(appContext.getString(R.string.error_generic, e.message ?: ""))
            }
    }


    fun getFirstProfileImageUrl(userId: String, onSuccess: (Uri) -> Unit, onFailure: () -> Unit) {
        val folderName = userId.removePrefix("profile_")
        val storageRef = FirebaseStorage.getInstance().reference.child(folderName)

        storageRef.listAll()
            .addOnSuccessListener { result ->
                val firstImage = result.items.firstOrNull()
                if (firstImage != null) {
                    firstImage.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri)
                    }.addOnFailureListener {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener {
                onFailure()
            }
    }

    fun getProfileData(userId: String, onSuccess: (name: String, photoUrl: String) -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        FirebaseFirestore.getInstance().collection("profiles")
            .document(profileId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Desconocido"
                val photoUrl = doc.getString("fotoPerfil") ?: ""
                onSuccess(name, photoUrl)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun uploadImage(userId: String, image: Any, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("$userId/${UUID.randomUUID()}.jpg")

        when (image) {
            is Bitmap -> {
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                val data = baos.toByteArray()

                storageRef.putBytes(data)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            onSuccess(uri.toString())
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }

            is Uri -> {
                storageRef.putFile(image)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            onSuccess(uri.toString())
                        }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
        }
    }

//    fun saveImageUrl(
//        userId: String,
//        imageUrl: String,
//        onSuccess: () -> Unit,
//        onFailure: (Exception) -> Unit,
//        context: Context
//    )
//        val imageData = hashMapOf("imageUrl" to imageUrl)
//        FirebaseFirestore.getInstance().collection("users")
//            .document(userId)
//            .set(imageData, SetOptions.merge())
//            .addOnSuccessListener {
//                Log.d("Firestore", context.getString(R.string.log_firestore_image_saved, imageUrl))
//                onSuccess()
//            }
//            .addOnFailureListener { e ->
//                Log.e("Firestore", context.getString(R.string.log_firestore_image_save_error, e.message ?: ""))
//                onFailure(e)
//            }
//    }

    fun loadUserImages1(userId: String, onImagesLoaded: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("$userId/")
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                val urls = mutableListOf<String>()
                val tasks = listResult.items.map { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        if (urls.size == listResult.items.size) {
                            onImagesLoaded(urls)
                        }
                    }
                }
                if (tasks.isEmpty()) onImagesLoaded(emptyList())
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Observa el estado de conexión del usuario y actualiza el estado online/offline en RTDB.
     * @param userId id del usuario sin prefijo.
     * @param onStatusUpdated callback para saber cuándo el estado cambió (opcional).
     * @return el ValueEventListener para poder removerlo después si es necesario.
     */


    fun observeUserConnectionStatus(
        context: Context,
        userId: String,
        onStatusUpdated: ((String) -> Unit)? = null
    ): ValueEventListener {

        val estadoRef = rtdbInstance.getReference("user/$userId/status")

        val listener = object : ValueEventListener {
            @SuppressLint("StringFormatInvalid")
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false

                if (connected) {
                    estadoRef.onDisconnect().setValue("inactive")
                        .addOnSuccessListener {
                            Log.d("FirebaseRepository", context.getString(R.string.log_on_disconnect_configured))
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", context.getString(R.string.log_error_configuring_on_disconnect, e.message ?: ""))
                        }

                    estadoRef.setValue("active")
                        .addOnSuccessListener {
                            Log.d("FirebaseRepository", context.getString(R.string.log_status_updated_active))
                            onStatusUpdated?.invoke("active")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", context.getString(R.string.log_error_updating_status_active, e.message ?: ""))
                            onStatusUpdated?.invoke("error")
                        }
                } else {
                    Log.d("FirebaseRepository", context.getString(R.string.log_not_connected_rtdb))
                    onStatusUpdated?.invoke("inactive")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepository", "Error en el listener RTDB: ${error.message}")
                onStatusUpdated?.invoke("error")
            }
        }

        connectedRef.addValueEventListener(listener)
        return listener
    }

    fun removeConnectionListener(listener: ValueEventListener) {
        connectedRef.removeEventListener(listener)
    }

    fun getMatchedUserId(
        context: Context,
        userId: String, // matchId
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure(Exception(context.getString(R.string.error_user_not_authenticated)))
            return
        }

        db.collection("matches")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val matchedUserId = doc.getString("matchedUserId")
                    val currentUserIdInMatch = doc.getString("currentUserId")
                    if (matchedUserId != null && currentUserIdInMatch != null) {
                        val otherUserId = if (currentUserId == currentUserIdInMatch) matchedUserId else currentUserIdInMatch
                        onSuccess("profile_$otherUserId")
                    } else {
                        onFailure(Exception(context.getString(R.string.error_matched_user_id_not_found)))
                    }
                } else {
                    onFailure(Exception(context.getString(R.string.error_matched_user_id_not_found)))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }


    fun getUserNameAndObserveStatus(
        context: Context,
        matchedUserId: String,
        onNameReceived: (String) -> Unit,
        onStatusChanged: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val profilesCollection = context.getString(R.string.collection_profiles)
        val fieldName = context.getString(R.string.field_name)
        val rtdbUserStatusPath = context.getString(R.string.rtdb_user_status_path)

        val profileId = if (matchedUserId.startsWith("profile_")) matchedUserId else "profile_$matchedUserId"

        db.collection("profiles")
            .document(profileId)
            .get()
            .addOnSuccessListener { profileDoc ->
                val nombreReal = profileDoc.getString(fieldName) ?: "Usuario"
                onNameReceived(nombreReal)

                val folderName = profileId.removePrefix(context.getString(R.string.prefix_profile))
                val statusPath = String.format(rtdbUserStatusPath, folderName)
                val estadoRef = rtdbInstance.getReference(statusPath)

                estadoRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val estado = snapshot.getValue(String::class.java)
                        if (estado != null) onStatusChanged(estado)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        onError(Exception(error.message))
                    }
                })
            }
            .addOnFailureListener { e -> onError(e) }
    }


fun getFirstProfileImage(matchedUserId: String, onSuccess: (Uri) -> Unit, onFailure: () -> Unit) {
        val folderName = matchedUserId.removePrefix("profile_")
        val storageRef = FirebaseStorage.getInstance().reference.child(folderName)

        storageRef.listAll()
            .addOnSuccessListener { result ->
                val primeraImagen = result.items.firstOrNull()
                if (primeraImagen != null) {
                    primeraImagen.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri)
                    }.addOnFailureListener {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { onFailure() }
    }

    fun sendTextMessage(
        matchId: String,
        senderId: String,
        texto: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val mensaje = MensajeEnviar(
            senderId = senderId,
            text = texto,
            timestamp = FieldValue.serverTimestamp(),
            type = "texto"
        )

        db.collection("matches")
            .document(matchId)
            .collection("messages")
            .document("mensaje_${System.currentTimeMillis()}")
            .set(mensaje)
            .addOnSuccessListener {
                // Actualizar lastMessage en subcolección matches para ambos usuarios
                db.collection("matches")
                    .document(matchId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentUserId = doc.getString("currentUserId") ?: return@addOnSuccessListener
                        val matchedUserId = doc.getString("matchedUserId") ?: return@addOnSuccessListener

                        val updateData = mapOf(
                            "lastMessage" to texto,
                            "timestamp" to FieldValue.serverTimestamp()
                        )

                        db.collection("profiles")
                            .document("profile_$currentUserId")
                            .collection("matches")
                            .document(matchId)
                            .update(updateData)
                            .addOnSuccessListener {
                                db.collection("profiles")
                                    .document("profile_$matchedUserId")
                                    .collection("matches")
                                    .document(matchId)
                                    .update(updateData)
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { e -> onFailure(e) }
                            }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun listenMessages(
        matchId: String,
        onNewMessage: (MensajeRecibir) -> Unit,
        onMessageUpdated: (MensajeRecibir) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("matches")
            .document(matchId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    for (dc in snapshots.documentChanges) {
                        val mensaje = dc.document.toObject(MensajeRecibir::class.java)
                        mensaje.id = dc.document.id
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                onNewMessage(mensaje)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                onMessageUpdated(mensaje)
                            }
                            else -> { /* No necesitas manejar REMOVED aquí */ }
                        }
                    }
                }
            }
    }


    fun fetchChatsForUser(
        currentUserId: String,
        onChatsLoaded: (List<HolderChats>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("profiles")
            .document("profile_$currentUserId")
            .collection("matches")
            .get()
            .addOnSuccessListener { documents ->
                val chatList = mutableListOf<HolderChats>()
                if (documents.isEmpty) {
                    onChatsLoaded(chatList)
                    return@addOnSuccessListener
                }

                val total = documents.size()
                var processed = 0

                for (doc in documents) {
                    val matchId = doc.id // Obtener matchId
                    val matchedUserId = doc.getString("matchedUserId") ?: run {
                        processed++
                        if (processed == total) onChatsLoaded(chatList)
                        return@addOnSuccessListener
                    }
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                    db.collection("profiles")
                        .document(matchedUserId)
                        .get()
                        .addOnSuccessListener { profileDoc ->
                            val nombreReal = profileDoc.getString("name") ?: matchedUserId

                            val chat = HolderChats(
                                matchId = matchId, // Incluir matchId
                                userId = matchedUserId,
                                username = nombreReal,
                                lastMessage = lastMessage,
                                timestamp = timestamp
                            )

                            chatList.add(chat)
                            processed++
                            if (processed == total) {
                                onChatsLoaded(chatList)
                            }
                        }
                        .addOnFailureListener { e ->
                            processed++
                            if (processed == total) {
                                onChatsLoaded(chatList)
                            }
                            Log.e("FirebaseRepository", "Error getting profile for $matchedUserId", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun fetchUserInterests(
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError(Exception("Usuario no autenticado"))
            return
        }
        val profileId = "profile_$uid"
        db.collection("profiles")
            .document(profileId)
            .get()
            .addOnSuccessListener { document ->
                val interests = document.get("interests") as? List<String> ?: emptyList()
                onSuccess(interests.map { it.replace("_", " ") }.toSet().toList())
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }


    fun fetchAllInterests(
        onSuccess: (Map<String, List<String>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("interests")
            .get()
            .addOnSuccessListener { snapshot ->
                val groups = mutableMapOf<String, List<String>>()
                for (doc in snapshot) {
                    val groupName = doc.id
                    val names = doc["name"] as? List<String> ?: emptyList()
                    groups[groupName] = names
                }
                onSuccess(groups)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun addInterestToDatabase(interest: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        val profileId = "profile_$uid"
        db.collection("profiles").document(profileId)
            .update("interests", FieldValue.arrayUnion(interest.replace(" ", "_")))
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    fun removeInterestFromDatabase(interest: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: return
        val profileId = "profile_$uid"
        db.collection("profiles").document(profileId)
            .update("interests", FieldValue.arrayRemove(interest.replace(" ", "_")))
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }


    fun saveUserInterests(
        interests: List<String>,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        val profileId = "profile_$uid"
        db.collection("profiles")
            .document(profileId)
            .set(mapOf("interests" to interests), SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    //Main activity

    fun loadInterests(
        onSuccess: (Map<String, List<String>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("interests").get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val data = mutableMapOf<String, List<String>>()
                for (doc in result) {
                    val groupName = doc.id
                    val names = doc["name"] as? List<String>
                    if (names != null) {
                        data[groupName] = names
                    }
                }
                onSuccess(data)
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun saveUserInterests(
        userId: String,
        interests: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val profileId = "profile_$userId"
        val perfilData = mapOf("interests" to interests)
        db.collection("profiles").document(profileId)
            .set(perfilData, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }


    // --------------- PROFILES ---------------

    fun loadProfiles(
        currentUserEmail: String?,
        onSuccess: (List<Profile>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (currentUserEmail == null) {
            onError(Exception("Usuario no autenticado"))
            return
        }
        db.collection("profiles").get()
            .addOnSuccessListener { result ->
                val profiles = mutableListOf<Profile>()
                val filteredDocs = result.documents.filter {
                    val email = it.getString("email")
                    email != null && email != currentUserEmail
                }
                if (filteredDocs.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                var loadedCount = 0
                filteredDocs.forEach { doc ->
                    val id = doc.id
                    val data = doc.data ?: emptyMap<String, Any>()
                    val name = data["name"] as? String ?: ""
                    val birthdate = data["birthdate"] as? String ?: ""
                    val gender = data["genero"] as? String ?: "Género desconocido"
                    val interests = data["interests"] as? List<String> ?: emptyList()

                    // Obtener encryptedLocation de la subcolección
                    db.collection("profiles").document(id).collection("location").document("actual").get()
                        .addOnSuccessListener { locationDoc ->
                            val encryptedLocation = locationDoc.getString("encryptedLocation") ?: ""

                            // Cargar imágenes
                            loadImageUrlsFromStorage(id, { images ->
                                profiles.add(Profile(id, name, birthdate, gender, encryptedLocation, interests, images))
                                loadedCount++
                                if (loadedCount == filteredDocs.size) {
                                    onSuccess(profiles)
                                }
                            }, { e ->
                                Log.e("FirebaseRepository", "Error loading images for $id", e)
                                loadedCount++
                                if (loadedCount == filteredDocs.size) {
                                    onSuccess(profiles)
                                }
                            })
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseRepository", "Error loading location for $id", e)
                            // Usar ubicación vacía si falla
                            loadImageUrlsFromStorage(id, { images ->
                                profiles.add(Profile(id, name, birthdate, gender, "", interests, images))
                                loadedCount++
                                if (loadedCount == filteredDocs.size) {
                                    onSuccess(profiles)
                                }
                            }, { e ->
                                loadedCount++
                                if (loadedCount == filteredDocs.size) {
                                    onSuccess(profiles)
                                }
                            })
                        }
                }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    fun loadImageUrlsFromStorage(
        profileId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (profileId.isBlank()) {
            onFailure(Exception("profileId vacío"))
            return
        }
        val userId = profileId.removePrefix("profile_")
        val ref = storage.reference.child(userId)
        ref.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                if (listResult.items.isEmpty()) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }
                val urls = mutableListOf<String>()
                var loadedCount = 0
                val total = listResult.items.size

                listResult.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        loadedCount++
                        if (loadedCount == total) onSuccess(urls)
                    }.addOnFailureListener { e ->
                        Log.e("FirebaseRepository", "Error cargando URL de ${item.name}", e)
                        loadedCount++
                        if (loadedCount == total) onSuccess(urls)
                    }
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }


    fun loadImageUrlsFromStorageMain(
        profileId: String,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.i("StorageLoad", "Attempting to load images for profileId: '$profileId'")

        if (profileId.isBlank()) {
            val ex = IllegalArgumentException("profileId is blank. Cannot create storage reference.")
            Log.e("StorageLoad", ex.message ?: "")
            onFailure(ex)
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference.child(profileId)
        Log.d("StorageLoad", "Storage reference path: ${storageRef.path}")

        storageRef.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                Log.i(
                    "StorageLoad",
                    "listAll SUCCESS for '$profileId'. Items found: ${listResult.items.size}, Prefixes: ${listResult.prefixes.size}"
                )
                if (listResult.items.isEmpty()) {
                    Log.w("StorageLoad", "No items (images) found in folder '$profileId'")
                    onSuccess(emptyList())
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
                                "All ${totalItems} URLs loaded for '$profileId'. Passing to onSuccess."
                            )
                            onSuccess(imageUrls)
                        }
                    }.addOnFailureListener { exception ->
                        Log.e(
                            "StorageLoad",
                            "FAILURE getting download URL for ${item.name} in '$profileId'",
                            exception
                        )
                        loadedCount++ // Incrementar para no bloquear la ejecución
                        if (loadedCount == totalItems) {
                            Log.w(
                                "StorageLoad",
                                "Finished processing all items for '$profileId' but some URLs failed. Passing partial list to onSuccess."
                            )
                            onSuccess(imageUrls)
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("StorageLoad", "listAll FAILURE for profileId: '$profileId'", exception)
                onFailure(exception)
            }
    }


    fun uploadUriToFirebaseStorage(userId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        // Generar un prefijo con timestamp en formato YYYYMMDD_HHMMSS
        val timestamp_ = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "$timestamp_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("$userId/$fileName")
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun uploadBitmapToFirebaseStorage(userId: String, bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        // Generar un prefijo con timestamp en formato YYYYMMDD_HHMMSS
        val timestamp_ = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "$timestamp_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("$userId/$fileName")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        val data = baos.toByteArray()
        storageRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
//    fun saveImageUrlToFirestore(userId: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        val imageData = hashMapOf("imageUrl" to imageUrl)
//        db.collection("users").document(userId)
//            .set(imageData, SetOptions.merge())
//            .addOnSuccessListener { onSuccess() }
//            .addOnFailureListener { e -> onFailure(e) }
//    }

    fun loadUserImages(userId: String, onSuccess: (List<Uri>) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = storage.reference.child("$userId/")
        storageRef.listAll().addOnSuccessListener { listResult ->
            val imageUris = mutableListOf<Uri>()
            // Ordenar los elementos por nombre (que incluye el timestamp)
            val sortedItems = listResult.items.sortedBy { it.name }
            val tasks = sortedItems.map { item ->
                item.downloadUrl
            }
            Tasks.whenAllSuccess<Uri>(tasks)
                .addOnSuccessListener { uris ->
                    imageUris.addAll(uris)
                    onSuccess(imageUris)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    fun loadUserInfo(userId: String, onSuccess: (Map<String, Any?>) -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(document.data ?: emptyMap())
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateDescription(userId: String, description: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .update("bio", description)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }


    fun updateUserProfile(userId: String, updates: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("profiles").document("profile_$userId").update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun loadGenders(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("gender")
            .get()
            .addOnSuccessListener { documents ->
                val genders = documents.mapNotNull { it.getString("name") }
                onSuccess(genders)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getDefaultGender(userId: String, fieldName: String, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(document.getString(fieldName))
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateGender(userId: String, fieldName: String, gender: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .update(fieldName, gender)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun loadNotificationSettings(userId: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .get()
            .addOnSuccessListener { document ->
                val notificationsEnabled = document.getBoolean("notificaciones") ?: false
                onSuccess(notificationsEnabled)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun updateNotificationSettings(userId: String, enabled: Boolean, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val profileId = "profile_$userId"
        db.collection("profiles").document(profileId)
            .update("notificaciones", enabled)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun countUserImages(userId: String, onSuccess: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = storage.reference.child("$userId/")
        storageRef.listAll()
            .addOnSuccessListener { result ->
                onSuccess(result.items.size)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ){
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
//
//    fun saveUserEdad(
//        userId: String,
//        ageMin: Int,
//        ageMax: Int,
//        onSuccess: () -> Unit,
//        onFailure: (Exception) -> Unit
//    ) {
//        db.collection("users").document(userId)
//            .set(mapOf(
//                "age_min" to ageMin,
//                "age_max" to ageMax
//                // Añade otros campos según sea necesario
//            ))
//            .addOnSuccessListener { onSuccess() }
//            .addOnFailureListener { e -> onFailure(e) }
//    }

    fun saveUserLocation(
        userId: String,
        encryptedLocation: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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
                latitude,
                longitude
            )

            if (shouldUpdateTime || shouldUpdateLocation) {
                val updateData = mapOf(
                    "encryptedLocation" to encryptedLocation,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                lastLocationDocRef.set(updateData)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e) }
            } else {
                onSuccess()
            }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }


    // javi momento

    fun createMatch(
        currentUserId: String,
        matchedUserId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Asegurar IDs sin prefijo
        val cleanCurrentUserId = currentUserId.removePrefix("profile_")
        val cleanMatchedUserId = matchedUserId.removePrefix("profile_")

        val matchId = if (cleanCurrentUserId < cleanMatchedUserId)
            "$cleanCurrentUserId-$cleanMatchedUserId"
        else "$cleanMatchedUserId-$cleanCurrentUserId"

        // Crear documento en colección matches
        val matchData = mapOf(
            "currentUserId" to cleanCurrentUserId,
            "matchedUserId" to cleanMatchedUserId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("matches")
            .document(matchId)
            .set(matchData)
            .addOnSuccessListener {
                // Actualizar subcolección matches en profiles para ambos usuarios
                val currentUserMatch = mapOf(
                    "matchedUserId" to "profile_$cleanMatchedUserId",
                    "lastMessage" to "",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                val matchedUserMatch = mapOf(
                    "matchedUserId" to "profile_$cleanCurrentUserId",
                    "lastMessage" to "",
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("profiles")
                    .document("profile_$cleanCurrentUserId")
                    .collection("matches")
                    .document(matchId)
                    .set(currentUserMatch)
                    .addOnSuccessListener {
                        db.collection("profiles")
                            .document("profile_$cleanMatchedUserId")
                            .collection("matches")
                            .document(matchId)
                            .set(matchedUserMatch)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e -> onFailure(e) }
                    }
                    .addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

}
