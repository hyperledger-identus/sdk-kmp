@file:Suppress("ktlint:standard:import-ordering")

package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import kotlinx.serialization.Serializable
import org.hyperledger.identus.walletsdk.domain.models.JWTVerifiableCredential

@Serializable
abstract class CredentialSubmission

@Serializable
class W3cCredentialSubmission(
    val comment: String? = null,
    val vc: JWTVerifiableCredential
) : CredentialSubmission()

@Serializable
class ProofCredentialSubmission(
    val context: String,
    val id: String,
    val type: Array<String>,
    val issuer: String,
    val issuanceDate: String,
    val credentialSubject: Map<String, String>,
    val proof: Proof
) : CredentialSubmission()
