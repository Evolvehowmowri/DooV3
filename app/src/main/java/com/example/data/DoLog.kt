package com.example.data

import androidx.room.Entity

@Entity(tableName = "do_logs", primaryKeys = ["doId", "date"])
data class DoLog(
    val doId: Int,
    val date: String, // "yyyy-MM-dd"
    val value: Float, // 1.0 for yes/no, or any custom logged value
    val loggedAt: Long = System.currentTimeMillis()
)
