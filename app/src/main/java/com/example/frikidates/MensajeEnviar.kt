import com.google.firebase.firestore.FieldValue

data class MensajeEnviar(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Any = FieldValue.serverTimestamp(),
    val type: String = "text"
)
