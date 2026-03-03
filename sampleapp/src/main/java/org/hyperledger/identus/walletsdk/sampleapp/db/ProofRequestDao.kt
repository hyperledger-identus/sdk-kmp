package org.hyperledger.identus.walletsdk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hyperledger.identus.walletsdk.sampleapp.db.PendingProofRequest

@Dao
interface ProofRequestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPending(request: PendingProofRequest)

    @Query("DELETE FROM pending_proof_request WHERE messageId = :messageId")
    fun deletePending(messageId: String)

    @Query("SELECT messageId FROM pending_proof_request")
    fun getAllIds(): List<String>
}
