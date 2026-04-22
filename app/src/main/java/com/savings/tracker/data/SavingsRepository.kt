package com.savings.tracker.data

import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val dao: SavingsDao) {

    val allAccounts: Flow<List<BankAccount>>    = dao.getAllAccounts()
    val allEntries: Flow<List<SavingsEntry>>    = dao.getAllEntries()
    val allEntriesAsc: Flow<List<SavingsEntry>> = dao.getAllEntriesAsc()
    val depositsAsc: Flow<List<SavingsEntry>>   = dao.getDepositsAsc()
    val allGoals: Flow<List<Goal>>              = dao.getAllGoals()
    val allTitheEntries: Flow<List<TitheEntry>> = dao.getAllTitheEntries()

    suspend fun addAccount(name: String, currency: String): Boolean {
        dao.insertAccount(BankAccount(name = name.trim(), currency = currency))
        return true
    }

    suspend fun deleteAccount(account: BankAccount) {
        dao.deleteEntriesForAccount(account.id)
        dao.deleteAccount(account)
    }

    /** Atomic deposit — updates balance and inserts entry in one transaction */
    suspend fun addDeposit(
        accountId: Int, amount: Double, currency: String,
        note: String, goalId: Int?, depositDate: Long
    ) {
        val account = dao.getAccountById(accountId) ?: return
        val updatedAccount = account.copy(balance = account.balance + amount)
        val entry = SavingsEntry(
            accountId       = accountId,
            amount          = amount,
            currency        = currency,
            note            = note.trim(),
            goalId          = goalId,
            transactionType = TransactionType.DEPOSIT,
            depositDate     = depositDate,
            createdAt       = System.currentTimeMillis()
        )
        dao.insertDepositTransaction(updatedAccount, entry)
    }

    /** Atomic withdrawal from account — reduces balance, logs withdrawal entry */
    suspend fun withdrawFromAccount(
        accountId: Int, amount: Double, currency: String,
        note: String, goalId: Int?, withdrawalDate: Long
    ): Boolean {
        val account = dao.getAccountById(accountId) ?: return false
        if (account.balance < amount) return false   // Safety: never go negative
        val updatedAccount = account.copy(balance = account.balance - amount)
        val entry = SavingsEntry(
            accountId       = accountId,
            amount          = amount,
            currency        = currency,
            note            = note.trim(),
            goalId          = goalId,
            transactionType = TransactionType.WITHDRAWAL,
            depositDate     = withdrawalDate,
            createdAt       = System.currentTimeMillis()
        )
        dao.insertWithdrawalTransaction(updatedAccount, entry)
        return true
    }

    /**
     * Withdraw from a goal (Option A):
     * Reduces balance of the specified account + logs withdrawal tagged to the goal.
     * Goal progress percentage drops automatically since saved amount decreases.
     */
    suspend fun withdrawFromGoal(
        goalId: Int, accountId: Int, amount: Double,
        currency: String, note: String, withdrawalDate: Long
    ): Boolean {
        return withdrawFromAccount(accountId, amount, currency, note, goalId, withdrawalDate)
    }

    /**
     * Delete a goal — two modes:
     * - withdrawFunds=false: just unlink deposits, money stays in accounts
     * - withdrawFunds=true: subtract total goal deposits from their accounts, then unlink
     */
    suspend fun deleteGoal(goal: Goal, withdrawFunds: Boolean) {
        if (withdrawFunds) {
            val allGoalEntries = dao.getAllEntriesForGoalOnce(goal.id)
            // Net = deposits minus withdrawals already made — prevents over-deduction
            val netByAccount = mutableMapOf<Int, Double>()
            allGoalEntries.forEach { entry ->
                val current = netByAccount.getOrDefault(entry.accountId, 0.0)
                netByAccount[entry.accountId] = if (entry.transactionType == TransactionType.DEPOSIT)
                    current + entry.amount else current - entry.amount
            }
            netByAccount.forEach { (accountId, netAmount) ->
                if (netAmount > 0) {
                    val account = dao.getAccountById(accountId) ?: return@forEach
                    val newBalance = (account.balance - netAmount).coerceAtLeast(0.0)
                    dao.updateAccount(account.copy(balance = newBalance))
                }
            }
        }
        dao.unlinkEntriesFromGoal(goal.id)
        dao.deleteGoal(goal)
    }

    fun getEntriesForAccount(accountId: Int): Flow<List<SavingsEntry>> =
        dao.getEntriesForAccount(accountId)

    fun getEntriesForGoal(goalId: Int): Flow<List<SavingsEntry>> =
        dao.getEntriesForGoal(goalId)

    suspend fun addGoal(goal: Goal) { dao.insertGoal(goal) }

    // Tithe
    suspend fun addTitheEntry(entry: TitheEntry) { dao.insertTitheEntry(entry) }

    suspend fun deleteTitheEntry(entry: TitheEntry) { dao.deleteTitheEntry(entry) }

    suspend fun reduceTitheEntry(entry: TitheEntry, newAmount: Double) {
        require(newAmount > 0) { "Amount must be positive" }
        require(newAmount < entry.donationAmount) { "New amount must be less than current" }
        // Bug 8 fix: recalculate percent so the history label stays accurate
        val newPercent = if (entry.paycheckAmount > 0)
            (newAmount / entry.paycheckAmount) * 100.0 else entry.donationPercent
        dao.updateTitheEntry(entry.id, newAmount, newPercent)
    }
}
