package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrainingHistory(
    val date: String = "",
    val trainingTitle: String = "",
    val durationMinutes: Int = 0,
    val completed: Boolean = false,
    val durationFormatted: String = "",
    val timestamp: Long = 0L
) : Parcelable
