package com.rania.trainup.com.rania.trainup

data class DailyTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "", // formato yyyy-mm-dd mirar esto
    val isCompleted: Boolean = false
)