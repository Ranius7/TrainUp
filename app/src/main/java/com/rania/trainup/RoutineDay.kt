package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoutineDay(
    val routineName: String = "",
    val muscleGroup: String = "",
    val comment: String? = null,
    val numExercises: Int = 0,
    val numSets: Int = 0,
    val exercises: List<Exercise> = emptyList() // Lista de ejercicios para este día
) : Parcelable