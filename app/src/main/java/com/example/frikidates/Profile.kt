package com.example.frikidates

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Profile(
    val id: String,
    val name: String,
    val birthdate: String,
    @PropertyName("genero")val gender: String,
    val encryptedLocation: String,
    val interests: List<String>,
    val images: List<String>
): Parcelable

