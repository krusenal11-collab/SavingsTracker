package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fix #11: Added depositDate (actual date of transfer), goalId (optional goal tag),
 * and currency (inherited from account at time of deposit for historical accuracy).
 */
@Entity(tableName = "savings_entries")
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: Int,
    val amount: Double,
    val currency: String = "USD",        // "USD" or "INR" — frozen at deposit time
    val note: String = "",
    val goalId: Int? = null,             // Which goal this deposit contributes to (optional)
    val depositDate: Long = System.currentTimeMillis(), // Actual date of transfer
    val createdAt: Long = System.currentTimeMillis()    // When user recorded it in app
)
