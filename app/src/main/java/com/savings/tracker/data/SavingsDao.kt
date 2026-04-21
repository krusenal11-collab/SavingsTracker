package com.savings.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {

    // ── Bank Accounts ──────────────────────────────────────────────────────

    @Query("SELECT * FROM bank_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): BankAccount?

    @Insert
    suspend fun insertAccount(account: BankAccount): Long

    @Update
    suspend fun updateAccount(account: BankAccount)

    @Delete
    suspend fun deleteAccount(account: BankAccount)

    // Fix #6: Removed SUM(balance) SQL query — total is now computed in-memory
    // after currency conversion so different currencies are handled correctly.

    // ── Savings Entries ────────────────────────────────────────────────────

    // Fix #19: Order by depositDate not createdAt
    @Query("SELECT * FROM savings_entries WHERE accountId = :accountId ORDER BY depositDate DESC")
    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>>

    // Fix #9 + #27: @Transaction ensures updateAccount + insertEntry are atomic
    @Transaction
    suspend fun insertDepositTransaction(account: BankAccount, entry: SavingsEntry) {
        updateAccount(account)
        insertEntry(entry)
    }

    @Insert
    suspend fun insertEntry(entry: SavingsEntry)

    // Fix #19: All feeds ordered by depositDate
    @Query("SELECT * FROM savings_entries ORDER BY depositDate DESC")
    fun getAllEntries(): Flow<List<SavingsEntry>>

    // Fix #18: Chart uses depositDate for X-axis ordering
    @Query("SELECT * FROM savings_entries ORDER BY depositDate ASC")
    fun getAllEntriesAsc(): Flow<List<SavingsEntry>>

    @Query("DELETE FROM savings_entries WHERE accountId = :accountId")
    suspend fun deleteEntriesForAccount(accountId: Int)

    @Query("SELECT * FROM savings_entries WHERE goalId = :goalId ORDER BY depositDate DESC")
    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>>

    // ── Goals ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Int): Goal?

    @Insert
    suspend fun insertGoal(goal: Goal): Long

    @Delete
    suspend fun deleteGoal(goal: Goal)

    // ── Tithe Entries ──────────────────────────────────────────────────────

    @Query("SELECT * FROM tithe_entries ORDER BY paycheckDate DESC")
    fun getAllTitheEntries(): Flow<List<TitheEntry>>

    @Insert
    suspend fun insertTitheEntry(entry: TitheEntry)
}
