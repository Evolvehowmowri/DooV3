package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String, // e.g. "#A8C7FA"
    val iconName: String, // e.g. "Favorite", "Work", "Home", "Psychology", "Brush", "Star"
    val orderIndex: Int = 0
)
