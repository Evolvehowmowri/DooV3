package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "affirmation_items",
    indices = [Index(value = ["categoryId"])]
)
data class AffirmationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
