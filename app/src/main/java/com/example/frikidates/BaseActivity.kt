package com.example.frikidates

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


open class BaseActivity : AppCompatActivity() {

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid.removePrefix("profile_")
        val estadoRef = FirebaseDatabase.getInstance("https://frikidatesdb-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("user/$userId/status")

        val connectedRef = FirebaseDatabase.getInstance("https://frikidatesdb-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("BaseActivity", "Snapshot recibido en .info/connected: $connected")

                if (connected) {
                    Log.d("BaseActivity", "Conectado a Firebase Realtime Database, actualizando estado...")

                    // Configurar onDisconnect para poner inactive cuando se desconecte
                    estadoRef.onDisconnect().setValue("inactive")
                        .addOnSuccessListener {
                            Log.d("BaseActivity", "onDisconnect configurado correctamente para poner 'inactive'")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BaseActivity", "Error configurando onDisconnect", e)
                        }

                    // Establecer activo ahora que está conectado
                    estadoRef.setValue("active")
                        .addOnSuccessListener {
                            Log.d("BaseActivity", "Estado actualizado a 'active' correctamente")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BaseActivity", "Error actualizando estado a 'active'", e)
                        }
                } else {
                    Log.d("BaseActivity", "No conectado a Firebase Realtime Database (connected = false)")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log o manejar error
            }
        })
    }

    override fun onStop() {
        super.onStop()
        // No es recomendable poner aquí inactive, porque el socket puede seguir abierto
        // Se puede quitar esta función para solo manejar presencia con .info/connected y onDisconnect
    }


}
