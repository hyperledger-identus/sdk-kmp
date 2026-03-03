package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Tests for Proof data class
 */
class ProofTest {

    @Test
    fun testProofCreation() {
        val proof = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = Proof.Purpose.AUTHENTICATION.value,
            verificationMethod = "did:prism:123#keys-1"
        )

        assertEquals("Ed25519Signature2018", proof.type)
        assertEquals("2024-01-15T10:00:00Z", proof.created)
        assertEquals("authentication", proof.proofPurpose)
        assertEquals("did:prism:123#keys-1", proof.verificationMethod)
        assertNull(proof.jws)
    }

    @Test
    fun testProofWithJWS() {
        val jwsSignature = "eyJhbGciOiJFZERTQSIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..test"
        val proof = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1",
            jws = jwsSignature
        )

        assertEquals(jwsSignature, proof.jws)
    }

    @Test
    fun testProofEquality() {
        val proof1 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1",
            jws = "signature"
        )
        val proof2 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1",
            jws = "signature"
        )

        assertEquals(proof1, proof2)
        assertEquals(proof1.hashCode(), proof2.hashCode())
    }

    @Test
    fun testProofInequalityDifferentType() {
        val proof1 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1"
        )
        val proof2 = Proof(
            type = "EcdsaSecp256k1Signature2019",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1"
        )

        assertNotEquals(proof1, proof2)
    }

    @Test
    fun testProofInequalityDifferentCreated() {
        val proof1 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1"
        )
        val proof2 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-16T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1"
        )

        assertNotEquals(proof1, proof2)
    }

    @Test
    fun testProofInequalityDifferentVerificationMethod() {
        val proof1 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:123#keys-1"
        )
        val proof2 = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = "authentication",
            verificationMethod = "did:prism:456#keys-1"
        )

        assertNotEquals(proof1, proof2)
    }

    @Test
    fun testProofPurposeEnumValue() {
        assertEquals("authentication", Proof.Purpose.AUTHENTICATION.value)
    }

    @Test
    fun testProofWithAuthenticationPurpose() {
        val proof = Proof(
            type = "Ed25519Signature2018",
            created = "2024-01-15T10:00:00Z",
            proofPurpose = Proof.Purpose.AUTHENTICATION.value,
            verificationMethod = "did:prism:123#keys-1"
        )

        assertEquals(Proof.Purpose.AUTHENTICATION.value, proof.proofPurpose)
    }
}
