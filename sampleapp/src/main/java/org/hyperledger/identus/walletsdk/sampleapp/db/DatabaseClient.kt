package org.hyperledger.identus.walletsdk.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseClient {
    @Volatile
    private var instance: AppDatabase? = null

    fun initializeInstance(context: Context) {
        if (instance == null) {
            synchronized(AppDatabase::class.java) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "database-name"
                    ).addMigrations(MIGRATION_1_2).build()
                }
            }
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_proof_request` (" +
                    "`messageId` TEXT NOT NULL, " +
                    "`thid` TEXT, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`messageId`)" +
                    ")"
            )
        }
    }

    fun getInstance(): AppDatabase {
        return instance ?: throw IllegalStateException("Database has not been initialized.")
    }
}
