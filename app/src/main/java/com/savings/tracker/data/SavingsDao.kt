package com.savings.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    // ── Bank Accounts ──────────────────────────────────────────────────────

    @Query("SELECT * FROM bank_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Int): BankAccount?

    @Insert
    suspend fun insertAccount(account: BankAccount): Long

    @Update
    suspend fun updateAccount(account: BankAccount)

    @Delete
    suspend fun deleteAccount(account: BankAccount)

    // Sum of all account balances → drives the "Total Savings" card
    @Query("SELECT COALESCE(SUM(balance), 0.0) FROM bank_accounts")
    fun getTotalSavings(): Flow<Double>

    // ── Savings Entries ────────────────────────────────────────────────────

    @Query("SELECT * FROM savings_entries WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>>

    @Insert
    suspend fun insertEntry(entry: SavingsEntry)

    // All entries across all accounts — newest first (for Summary feed)
    @Query("SELECT * FROM savings_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<SavingsEntry>>

    // All entries oldest first — for building the cumulative chart
    @Query("SELECT * FROM savings_entries ORDER BY createdAt ASC")
    fun getAllEntriesAsc(): Flow<List<SavingsEntry>>

    // Called when an account is deleted — removes all its deposit history too
    @Query("DELETE FROM savings_entries WHERE accountId = :accountId")
    suspend fun deleteEntriesForAccount(accountId: Int)
}
