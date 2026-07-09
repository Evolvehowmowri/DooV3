package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "do_nothing_logs")
data class DoNothingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val targetMinutes: Int,
    val actualSeconds: Int,
    val categoryName: String,
    val sentencesCount: Int,
    val sentencesLogged: String // Summary of sentence details: "Sentence 1 (4s), Sentence 2 (11s)..."
)
