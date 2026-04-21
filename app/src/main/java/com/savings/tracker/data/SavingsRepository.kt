package com.savings.tracker.data

import kotlinx.coroutines.flow.Flow

/**
 * The repository is the single point of contact between the ViewModel
 * and the database. The ViewModel never talks to the DAO directly.
 */
class SavingsRepository(private val dao: SavingsDao) {

    val allAccounts: Flow<List<BankAccount>> = dao.getAllAccounts()
    val totalSavings: Flow<Double> = dao.getTotalSavings()
    val allEntries: Flow<List<SavingsEntry>> = dao.getAllEntries()
    val allEntriesAsc: Flow<List<SavingsEntry>> = dao.getAllEntriesAsc()

    suspend fun addAccount(name: String) {
        dao.insertAccount(BankAccount(name = name.trim()))
    }

    suspend fun deleteAccount(account: BankAccount) {
        dao.deleteEntriesForAccount(account.id) // delete history first to avoid orphaned rows
        dao.deleteAccount(account)
    }

    /**
     * Log a deposit: updates the account balance and saves the entry
     * so it shows up in the deposit history.
     */
    suspend fun addDeposit(accountId: Int, amount: Double, note: String) {
        val account = dao.getAccountById(accountId) ?: return
        dao.updateAccount(account.copy(balance = account.balance + amount))
        dao.insertEntry(
            SavingsEntry(accountId = accountId, amount = amount, note = note.trim())
        )
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        dao.getEntriesForAccount(accountId)
}
