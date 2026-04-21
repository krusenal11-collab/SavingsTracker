package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tithe_entries")
data class TitheEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val paycheckAmount: Double,
    val paycheckCurrency: String = "INR",
    val donationPercent: Double,         // e.g. 10.0 for 10%
    val donationAmount: Double,          // calculated: paycheckAmount * donationPercent / 100
    val accountId: Int,                  // which account the money is in
    val paycheckDate: Long,              // date salary was credited
    val createdAt: Long = System.currentTimeMillis()
)
