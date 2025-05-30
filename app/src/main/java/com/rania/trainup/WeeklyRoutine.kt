package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WeeklyRoutine(
    val clientUid: String = "",
    val trainerUid: String = "",
    val routineDays: List<RoutineDay> = emptyList(),
    val published: Boolean = false
) : Parcelable

