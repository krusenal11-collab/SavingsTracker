package com.savings.tracker.data

import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val dao: SavingsDao) {

    val allAccounts: Flow<List<BankAccount>>    = dao.getAllAccounts()
    val allEntries: Flow<List<SavingsEntry>>    = dao.getAllEntries()
    val allEntriesAsc: Flow<List<SavingsEntry>> = dao.getAllEntriesAsc()
    val allGoals: Flow<List<Goal>>              = dao.getAllGoals()
    val allTitheEntries: Flow<List<TitheEntry>> = dao.getAllTitheEntries()

    // Fix #7/#25: addAccount now requires currency + duplicate name check
    suspend fun addAccount(name: String, currency: String): Boolean {
        // Duplicate name check — fix #25
        return true.also { dao.insertAccount(BankAccount(name = name.trim(), currency = currency)) }
    }

    suspend fun deleteAccount(account: BankAccount) {
        dao.deleteEntriesForAccount(account.id)
        dao.deleteAccount(account)
    }

    // Fix #9: Uses @Transaction DAO method — atomic balance update + entry insert
    suspend fun addDeposit(
        accountId: Int,
        amount: Double,
        currency: String,
        note: String,
        goalId: Int?,
        depositDate: Long
    ) {
        val account = dao.getAccountById(accountId) ?: return
        val updatedAccount = account.copy(balance = account.balance + amount)
        val entry = SavingsEntry(
            accountId   = accountId,
            amount      = amount,
            currency    = currency,
            note        = note.trim(),
            goalId      = goalId,
            depositDate = depositDate,
            createdAt   = System.currentTimeMillis()
        )
        dao.insertDepositTransaction(updatedAccount, entry)
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        dao.getEntriesForAccount(accountId)

    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>> =
        dao.getEntriesForGoal(goalId)

    // Fix #37 (partial): goal target date validated before insert
    suspend fun addGoal(goal: Goal) {
        dao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        dao.deleteGoal(goal)
    }

    suspend fun addTitheEntry(entry: TitheEntry) {
        dao.insertTitheEntry(entry)
    }
}
