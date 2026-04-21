package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val emoji: String = "🎯",
    val targetAmount: Double,
    val targetCurrency: String = "INR",
    val targetDate: Long,                // Must be in the future — fix #37 validated in UI
    val createdAt: Long = System.currentTimeMillis()
)
