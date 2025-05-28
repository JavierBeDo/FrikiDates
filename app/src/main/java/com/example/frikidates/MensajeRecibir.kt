package com.example.frikidates

import java.util.Date

data class MensajeRecibir(
    var id: String = "",
    var senderId: String = "",
    var text: String = "",
    var timestamp: com.google.firebase.Timestamp? = null,
    var type: String = "texto"
)
