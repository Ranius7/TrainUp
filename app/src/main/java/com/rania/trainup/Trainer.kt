package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // Añadido para poder pasar objetos Trainer entre Activities
data class Trainer(
    val name: String = "",
    val email: String = "",
    // val password: String = "", // La contraseña no se guarda en Firestore
    val maxClients: Int = 0,
    val uid: String = "", // El UID de Firebase Auth para este entrenador
    val role: String = MainActivity.ROLE_TRAINER
) : Parcelable