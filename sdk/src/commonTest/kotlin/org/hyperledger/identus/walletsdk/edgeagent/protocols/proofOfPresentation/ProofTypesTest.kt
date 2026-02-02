package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for ProofTypes data class
 */
class ProofTypesTest {

    @Test
    fun testProofTypesCreation() {
        val proofTypes = ProofTypes(schema = "https://example.com/schema/1.0")

        assertEquals("https://example.com/schema/1.0", proofTypes.schema)
        assertEquals(null, proofTypes.requiredFields)
        assertEquals(null, proofTypes.trustIssuers)
    }

    @Test
    fun testProofTypesWithRequiredFields() {
        val requiredFields = arrayOf("name", "age", "address")
        val proofTypes = ProofTypes(
            schema = "https://example.com/schema/identity",
            requiredFields = requiredFields
        )

        assertEquals("https://example.com/schema/identity", proofTypes.schema)
        assertTrue(proofTypes.requiredFields!!.contentEquals(requiredFields))
    }

    @Test
    fun testProofTypesWithTrustIssuers() {
        val trustIssuers = arrayOf("did:prism:issuer1", "did:prism:issuer2")
        val proofTypes = ProofTypes(
            schema = "https://example.com/schema/certificate",
            trustIssuers = trustIssuers
        )

        assertEquals("https://example.com/schema/certificate", proofTypes.schema)
        assertTrue(proofTypes.trustIssuers!!.contentEquals(trustIssuers))
    }

    @Test
    fun testProofTypesWithAllFields() {
        val requiredFields = arrayOf("firstName", "lastName")
        val trustIssuers = arrayOf("did:prism:trusted-issuer")
        val proofTypes = ProofTypes(
            schema = "https://example.com/schema/full",
            requiredFields = requiredFields,
            trustIssuers = trustIssuers
        )

        assertEquals("https://example.com/schema/full", proofTypes.schema)
        assertTrue(proofTypes.requiredFields!!.contentEquals(requiredFields))
        assertTrue(proofTypes.trustIssuers!!.contentEquals(trustIssuers))
    }

    @Test
    fun testProofTypesEquality() {
        val proofTypes1 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = arrayOf("field1", "field2"),
            trustIssuers = arrayOf("issuer1")
        )
        val proofTypes2 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = arrayOf("field1", "field2"),
            trustIssuers = arrayOf("issuer1")
        )

        assertEquals(proofTypes1, proofTypes2)
        assertEquals(proofTypes1.hashCode(), proofTypes2.hashCode())
    }

    @Test
    fun testProofTypesInequalityDifferentSchema() {
        val proofTypes1 = ProofTypes(schema = "https://example.com/schema1")
        val proofTypes2 = ProofTypes(schema = "https://example.com/schema2")

        assertNotEquals(proofTypes1, proofTypes2)
    }

    @Test
    fun testProofTypesInequalityDifferentRequiredFields() {
        val proofTypes1 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = arrayOf("field1")
        )
        val proofTypes2 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = arrayOf("field2")
        )

        assertNotEquals(proofTypes1, proofTypes2)
    }

    @Test
    fun testProofTypesInequalityNullVsNonNullRequiredFields() {
        val proofTypes1 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = null
        )
        val proofTypes2 = ProofTypes(
            schema = "https://example.com/schema",
            requiredFields = arrayOf("field1")
        )

        assertNotEquals(proofTypes1, proofTypes2)
    }

    @Test
    fun testProofTypesInequalityDifferentTrustIssuers() {
        val proofTypes1 = ProofTypes(
            schema = "https://example.com/schema",
            trustIssuers = arrayOf("issuer1")
        )
        val proofTypes2 = ProofTypes(
            schema = "https://example.com/schema",
            trustIssuers = arrayOf("issuer2")
        )

        assertNotEquals(proofTypes1, proofTypes2)
    }

    @Test
    fun testProofTypesInequalityNullVsNonNullTrustIssuers() {
        val proofTypes1 = ProofTypes(
            schema = "https://example.com/schema",
            trustIssuers = null
        )
        val proofTypes2 = ProofTypes(
            schema = "https://example.com/schema",
            trustIssuers = arrayOf("issuer1")
        )

        assertNotEquals(proofTypes1, proofTypes2)
    }

    @Test
    fun testProofTypesEqualityBothNullOptionalFields() {
        val proofTypes1 = ProofTypes(schema = "schema")
        val proofTypes2 = ProofTypes(schema = "schema")

        assertEquals(proofTypes1, proofTypes2)
        assertEquals(proofTypes1.hashCode(), proofTypes2.hashCode())
    }
}
