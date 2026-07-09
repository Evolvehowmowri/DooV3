package com.example.data

data class NotificationActionItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "MEASUREMENT", "AFFIRMATION", "DO_NOTHING"
    val targetScreen: String, // "MEASUREMENT_TRACKER", "AFFIRMATION", "DO_NOTHING"
    val trackerId: Long? = null,
    val priority: Int = 2 // 1 = High, 2 = Medium, 3 = Low
)
