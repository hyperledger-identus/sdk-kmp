package org.hyperledger.identus.walletsdk.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.hyperledger.identus.walletsdk.sampleapp.db.Message
import org.hyperledger.identus.walletsdk.sampleapp.db.PendingProofRequest

@Database(entities = [Message::class, PendingProofRequest::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun proofRequestDao(): ProofRequestDao
}
