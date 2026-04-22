package com.savings.tracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE bank_accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN goalId INTEGER")
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN depositDate INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE savings_entries SET depositDate = createdAt WHERE depositDate = 0")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                emoji TEXT NOT NULL DEFAULT '🎯',
                targetAmount REAL NOT NULL,
                targetCurrency TEXT NOT NULL DEFAULT 'INR',
                targetDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tithe_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                paycheckAmount REAL NOT NULL,
                paycheckCurrency TEXT NOT NULL DEFAULT 'INR',
                donationPercent REAL NOT NULL,
                donationAmount REAL NOT NULL,
                accountId INTEGER NOT NULL,
                paycheckDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

/** Migration v2 → v3: adds transactionType to savings_entries for withdrawal support */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // All existing entries are deposits — default is safe
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'DEPOSIT'")
    }
}

@Database(
    entities = [BankAccount::class, SavingsEntry::class, Goal::class, TitheEntry::class],
    version = 3,
    exportSchema = false
)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao

    companion object {
        @Volatile private var INSTANCE: SavingsDatabase? = null

        fun getDatabase(context: Context): SavingsDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SavingsDatabase::class.java,
                    "savings_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
