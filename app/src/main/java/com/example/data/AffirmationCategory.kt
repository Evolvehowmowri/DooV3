package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "affirmation_categories")
data class AffirmationCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
