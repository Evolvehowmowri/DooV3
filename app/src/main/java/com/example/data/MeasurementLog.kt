package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "measurement_logs",
    indices = [Index(value = ["trackerId"])]
)
data class MeasurementLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackerId: Long,
    val value: Double,
    val timestamp: Long, // Custom user-selected timestamp
    val notes: String = ""
)
