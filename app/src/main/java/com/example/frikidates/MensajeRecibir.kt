package com.example.frikidates

data class MensajeRecibir(
    val nombre: String,          // Nombre del remitente
    val mensaje: String,         // Contenido del mensaje
    val hora: Long,              // Hora de envío en milisegundos
    val type_mensaje: String,    // Tipo de mensaje ("1" para texto, "2" para imagen)
    val urlFoto: String?,        // URL de la imagen del mensaje (si hay)
    val fotoPerfil: String?      // URL de la imagen de perfil del remitente (si hay)
)
