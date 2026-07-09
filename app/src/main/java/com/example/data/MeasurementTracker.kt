package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurement_trackers")
data class MeasurementTracker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val unit: String,
    val initialValue: Double,
    val targetValue: Double,
    val milestonesStr: String = "", // Comma-separated list of milestone values (e.g., "100,99,98,96,94,91,89,87,84")
    val trackingInterval: String = "Weekly", // Daily, Weekly, Bi-weekly, Monthly, Ad-hoc
    val createdAt: Long = System.currentTimeMillis()
)
