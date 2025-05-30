package com.rania.trainup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Trainer(
    val name: String = "",
    val email: String = "",
    val maxClients: Int = 0,
    val uid: String = "",
    val role: String = MainActivity.ROLE_TRAINER
) : Parcelable