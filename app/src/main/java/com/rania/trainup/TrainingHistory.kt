package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrainingHistory(
    val date: String = "", // Formato "DD MM" o "YYYY-MM-DD"
    val trainingTitle: String = "", // Ej: "Glúteos"
    val durationMinutes: Int = 0,
    val completed: Boolean = false
) : Parcelable