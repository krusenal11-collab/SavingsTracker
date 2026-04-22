package com.savings.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Transaction types for clarity in history feeds */
object TransactionType {
    const val DEPOSIT    = "DEPOSIT"
    const val WITHDRAWAL = "WITHDRAWAL"
}

@Entity(tableName = "savings_entries")
data class SavingsEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: Int,
    val amount: Double,                                      // Always positive — type determines direction
    val currency: String = "USD",
    val note: String = "",
    val goalId: Int? = null,
    val transactionType: String = TransactionType.DEPOSIT,   // v2→v3: DEPOSIT or WITHDRAWAL
    val depositDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
