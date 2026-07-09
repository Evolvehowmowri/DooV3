package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dos")
data class Do(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "TASK" or "RECURRING"
    val isMeasurable: Boolean = false,
    val targetValue: Float = 1f,
    val unit: String = "",
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val category: String = "General", // "Health", "Work", "Personal", etc.
    val reminderTime: String? = null, // e.g. "08:30"
    val targetDate: String? = null, // "yyyy-MM-dd" (for TASK)
    val targetTime: String? = null, // "HH:mm" for sorting/scheduled time
    val purpose: String = "", // Why I want to do something
    val benefit: String = "", // What's the benefit
    val cost: String = "", // What's the cost / obstacles
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
