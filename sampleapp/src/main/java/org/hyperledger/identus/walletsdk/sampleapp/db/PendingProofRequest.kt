package org.hyperledger.identus.walletsdk.sampleapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_proof_request")
data class PendingProofRequest(
    @PrimaryKey
    val messageId: String,
    val thid: String?,
    val createdAt: Long
)
