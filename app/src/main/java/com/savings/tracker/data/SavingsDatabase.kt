package com.savings.tracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Fix #14: Database migration v1 → v2.
 * Adds currency to bank_accounts, adds depositDate/goalId/currency to savings_entries,
 * creates goals and tithe_entries tables.
 * Existing data is preserved — no destructive migration.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add currency column to bank_accounts
        database.execSQL("ALTER TABLE bank_accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")

        // Add new columns to savings_entries
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN goalId INTEGER")
        // depositDate defaults to createdAt so existing history stays correctly ordered
        database.execSQL("ALTER TABLE savings_entries ADD COLUMN depositDate INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE savings_entries SET depositDate = createdAt WHERE depositDate = 0")

        // Create goals table
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

        // Create tithe_entries table
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

@Database(
    entities = [BankAccount::class, SavingsEntry::class, Goal::class, TitheEntry::class],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
