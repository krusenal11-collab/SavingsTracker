package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one savings account (e.g. "Chase Savings", "Marcus").
 * The balance is updated every time a deposit is logged.
 */
@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
