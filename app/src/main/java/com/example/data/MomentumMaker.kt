package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "momentum_makers")
data class MomentumMaker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // e.g. "Clean", "Mind", "Body", "Admin", "Social"
    val durationMinutes: Int, // e.g. 1, 2, 5, 10
    val createdAt: Long = System.currentTimeMillis()
)
