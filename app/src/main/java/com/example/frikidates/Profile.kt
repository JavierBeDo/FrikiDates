package com.example.frikidates

import com.google.firebase.firestore.PropertyName

data class Profile(
    val name: String,
    //@PropertyName("birthdate")val birthday: String,
    val age: Int,
    @PropertyName("genero")val gender: String,
    val city: String,
    val compatibility: String,
    //val interests: List<String>,
    val images: List<String>

    
)

