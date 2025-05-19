package com.example.frikidates

import java.util.Date

data class MensajeRecibir(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Date? = null,
    val type: String = "text" // "text" o "image"
)
