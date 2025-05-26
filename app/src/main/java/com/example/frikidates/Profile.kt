package com.example.frikidates

import com.google.firebase.firestore.PropertyName

data class Profile(
    val profileId: String,  // <-- Agrega este campo
    val name: String,
    val age: Int,
    @PropertyName("genero")val gender: String,
    val city: String,
    val compatibility: String,
    val images: List<String>
)
