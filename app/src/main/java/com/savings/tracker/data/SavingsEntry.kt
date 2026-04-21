package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single deposit transaction into a savings account.
 * Used to build the deposit history shown in Account Detail screen.
 */
@Entity(tableName = "savings_entries")
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: Int,          // Which account this deposit belongs to
    val amount: Double,          // How much was deposited
    val note: String = "",       // Optional note (e.g. "Weekly transfer")
    val createdAt: Long = System.currentTimeMillis()
)
