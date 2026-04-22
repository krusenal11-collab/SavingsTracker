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

    // ── Savings Entries ────────────────────────────────────────────────────

    @Query("SELECT * FROM savings_entries WHERE accountId = :accountId ORDER BY depositDate DESC")
    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>>

    @Query("SELECT * FROM savings_entries ORDER BY depositDate DESC")
    fun getAllEntries(): Flow<List<SavingsEntry>>

    @Query("SELECT * FROM savings_entries ORDER BY depositDate ASC")
    fun getAllEntriesAsc(): Flow<List<SavingsEntry>>

    @Query("SELECT * FROM savings_entries WHERE goalId = :goalId ORDER BY depositDate DESC")
    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>>

    // Deposits only — used for chart (withdrawals don't add to cumulative total)
    @Query("SELECT * FROM savings_entries WHERE transactionType = 'DEPOSIT' ORDER BY depositDate ASC")
    fun getDepositsAsc(): Flow<List<SavingsEntry>>

    @Insert
    suspend fun insertEntry(entry: SavingsEntry)

    @Delete
    suspend fun deleteEntry(entry: SavingsEntry)

    // Atomic deposit: update balance + insert entry in one transaction
    @Transaction
    suspend fun insertDepositTransaction(account: BankAccount, entry: SavingsEntry) {
        updateAccount(account)
        insertEntry(entry)
    }

    // Atomic withdrawal: update balance + insert withdrawal entry
    @Transaction
    suspend fun insertWithdrawalTransaction(account: BankAccount, entry: SavingsEntry) {
        updateAccount(account)
        insertEntry(entry)
    }

    @Query("DELETE FROM savings_entries WHERE accountId = :accountId")
    suspend fun deleteEntriesForAccount(accountId: Int)

    // When a goal is deleted, unlink its deposits (set goalId to null)
    @Query("UPDATE savings_entries SET goalId = NULL WHERE goalId = :goalId")
    suspend fun unlinkEntriesFromGoal(goalId: Int)

    // All entries (deposits + withdrawals) for a goal — used for net calculation on delete
    @Query("SELECT * FROM savings_entries WHERE goalId = :goalId")
    suspend fun getAllEntriesForGoalOnce(goalId: Int): List<SavingsEntry>

    // Deposits only for a goal — kept for backward compat
    @Query("SELECT * FROM savings_entries WHERE goalId = :goalId AND transactionType = 'DEPOSIT'")
    suspend fun getDepositEntriesForGoalOnce(goalId: Int): List<SavingsEntry>

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

    @Delete
    suspend fun deleteTitheEntry(entry: TitheEntry)

    // Bug 8 fix: also update donationPercent so history label stays accurate
    @Query("UPDATE tithe_entries SET donationAmount = :newAmount, donationPercent = :newPercent WHERE id = :id")
    suspend fun updateTitheEntry(id: Int, newAmount: Double, newPercent: Double)
}
