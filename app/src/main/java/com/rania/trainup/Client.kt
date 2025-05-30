package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Client(
    val name: String = "",
    val email: String = "",
    // val password: String = "", /
    val objective: String = "",
    val trainerUid: String = "",
    val uid: String = "",
    val isNew: Boolean = true,
    val role: String = "CLIENT"
) : Parcelable