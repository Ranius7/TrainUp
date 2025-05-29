package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Exercise(
    val name: String = "",
    val material: String = "",
    val series: Int = 0,
    val repetitions: Int = 0,
    val rest: Int = 0, // En segundos
    val description: String = "",
    val stability: Int = 0
) : Parcelable