package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoutineDay(
    val dayOfWeek: String = "",
    val muscleGroup: String = "",
    val numExercises: Int = 0,
    val numSets: Int = 0,
    val exercises: List<Exercise> = emptyList() // Lista de ejercicios para este día
) : Parcelable