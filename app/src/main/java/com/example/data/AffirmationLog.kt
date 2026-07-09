package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "affirmation_logs",
    indices = [Index(value = ["categoryId"])]
)
data class AffirmationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    val categoryName: String,
    val affirmationText: String,
    val tapCount: Int,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)
