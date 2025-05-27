package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // Añadido para poder pasar objetos Client entre Activities
data class Client(
    val name: String = "",
    val email: String = "",
    // val password: String = "", // La contraseña NO debe guardarse en Firestore, solo Firebase Auth la maneja
    val objective: String = "",
    val trainerUid: String = "", // Almacenar el UID del entrenador, no el email
    val uid: String = "", // El UID de Firebase Auth para este cliente
    val isNew: Boolean = true, // Para marcar si es un cliente nuevo para el entrenador
    val role: String = "CLIENT" // <-- Añade este campo por defecto
) : Parcelable