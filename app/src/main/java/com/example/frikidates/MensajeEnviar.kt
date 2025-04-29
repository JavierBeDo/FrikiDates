package com.example.frikidates

data class MensajeEnviar(
    var hora: Map<String, Any>? = null
) : Mensaje() {

    constructor(
        mensaje: String?,
        nombre: String?,
        fotoPerfil: String?,
        type_mensaje: String?,
        hora: Map<String, Any>?
    ) : this(hora) {
        this.mensaje = mensaje
        this.nombre = nombre
        this.fotoPerfil = fotoPerfil
        this.type_mensaje = type_mensaje
    }

}
