package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fix #2: Added `currency` field ("USD" or "INR"). */
@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val balance: Double = 0.0,
    val currency: String = "USD",   // "USD" or "INR"
    val createdAt: Long = System.currentTimeMillis()
)
