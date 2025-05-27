package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Goal(
    val id: String = "", // Para identificar el objetivo en Firestore
    val text: String = "",
    val isCompleted: Boolean = false
) : Parcelable