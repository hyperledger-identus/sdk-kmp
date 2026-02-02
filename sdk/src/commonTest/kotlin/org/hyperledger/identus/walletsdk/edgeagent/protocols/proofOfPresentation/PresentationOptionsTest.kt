package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import org.hyperledger.identus.walletsdk.domain.models.CredentialType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for PresentationOptions implementations:
 * - JWTPresentationOptions
 * - AnoncredsPresentationOptions
 * - SDJWTPresentationOptions
 */
class PresentationOptionsTest {

    // JWTPresentationOptions tests
    @Test
    fun testJWTPresentationOptionsCreation() {
        val options = JWTPresentationOptions(
            domain = "example.com",
            challenge = "challenge-123"
        )

        assertEquals("Presentation", options.name)
        assertEquals("Presentation definition", options.purpose)
        assertEquals("example.com", options.domain)
        assertEquals("challenge-123", options.challenge)
        assertTrue(options.jwt.contentEquals(arrayOf("ES256K")))
        assertEquals(CredentialType.JWT, options.type)
    }

    @Test
    fun testJWTPresentationOptionsWithCustomValues() {
        val options = JWTPresentationOptions(
            name = "Custom Presentation",
            purpose = "Custom purpose",
            jwt = arrayOf("ES256K", "EdDSA"),
            domain = "custom.domain.com",
            challenge = "custom-challenge"
        )

        assertEquals("Custom Presentation", options.name)
        assertEquals("Custom purpose", options.purpose)
        assertEquals("custom.domain.com", options.domain)
        assertEquals("custom-challenge", options.challenge)
        assertTrue(options.jwt.contentEquals(arrayOf("ES256K", "EdDSA")))
    }

    @Test
    fun testJWTPresentationOptionsEquality() {
        val options1 = JWTPresentationOptions(
            name = "Test",
            purpose = "Test purpose",
            jwt = arrayOf("ES256K"),
            domain = "test.com",
            challenge = "challenge"
        )
        val options2 = JWTPresentationOptions(
            name = "Test",
            purpose = "Test purpose",
            jwt = arrayOf("ES256K"),
            domain = "test.com",
            challenge = "challenge"
        )

        assertEquals(options1, options2)
        assertEquals(options1.hashCode(), options2.hashCode())
    }

    @Test
    fun testJWTPresentationOptionsInequalityDifferentDomain() {
        val options1 = JWTPresentationOptions(
            domain = "domain1.com",
            challenge = "challenge"
        )
        val options2 = JWTPresentationOptions(
            domain = "domain2.com",
            challenge = "challenge"
        )

        assertNotEquals(options1, options2)
    }

    @Test
    fun testJWTPresentationOptionsInequalityDifferentChallenge() {
        val options1 = JWTPresentationOptions(
            domain = "domain.com",
            challenge = "challenge1"
        )
        val options2 = JWTPresentationOptions(
            domain = "domain.com",
            challenge = "challenge2"
        )

        assertNotEquals(options1, options2)
    }

    @Test
    fun testJWTPresentationOptionsInequalityDifferentJwt() {
        val options1 = JWTPresentationOptions(
            domain = "domain.com",
            challenge = "challenge",
            jwt = arrayOf("ES256K")
        )
        val options2 = JWTPresentationOptions(
            domain = "domain.com",
            challenge = "challenge",
            jwt = arrayOf("EdDSA")
        )

        assertNotEquals(options1, options2)
    }

    @Test
    fun testJWTPresentationOptionsImplementsPresentationOptions() {
        val options: PresentationOptions = JWTPresentationOptions(
            domain = "test.com",
            challenge = "test"
        )

        assertEquals(CredentialType.JWT, options.type)
    }

    // AnoncredsPresentationOptions tests
    @Test
    fun testAnoncredsPresentationOptionsCreation() {
        val options = AnoncredsPresentationOptions(nonce = "nonce-123")

        assertEquals("nonce-123", options.nonce)
        assertEquals(CredentialType.ANONCREDS_PROOF_REQUEST, options.type)
    }

    @Test
    fun testAnoncredsPresentationOptionsEquality() {
        val options1 = AnoncredsPresentationOptions(nonce = "same-nonce")
        val options2 = AnoncredsPresentationOptions(nonce = "same-nonce")

        assertEquals(options1, options2)
        assertEquals(options1.hashCode(), options2.hashCode())
    }

    @Test
    fun testAnoncredsPresentationOptionsInequalityDifferentNonce() {
        val options1 = AnoncredsPresentationOptions(nonce = "nonce1")
        val options2 = AnoncredsPresentationOptions(nonce = "nonce2")

        assertNotEquals(options1, options2)
    }

    @Test
    fun testAnoncredsPresentationOptionsImplementsPresentationOptions() {
        val options: PresentationOptions = AnoncredsPresentationOptions(nonce = "test")

        assertEquals(CredentialType.ANONCREDS_PROOF_REQUEST, options.type)
    }

    // SDJWTPresentationOptions tests
    @Test
    fun testSDJWTPresentationOptionsCreation() {
        val options = SDJWTPresentationOptions()

        assertEquals("Presentation", options.name)
        assertEquals("Presentation definition", options.purpose)
        assertEquals("", options.domain)
        assertEquals("", options.challenge)
        assertTrue(options.sdjwt.contentEquals(arrayOf("EdDSA")))
        assertEquals(CredentialType.SDJWT, options.type)
    }

    @Test
    fun testSDJWTPresentationOptionsWithCustomValues() {
        val options = SDJWTPresentationOptions(
            name = "SD-JWT Presentation",
            purpose = "SD-JWT verification",
            sdjwt = arrayOf("ES256", "EdDSA"),
            domain = "sdjwt.domain.com",
            challenge = "sdjwt-challenge"
        )

        assertEquals("SD-JWT Presentation", options.name)
        assertEquals("SD-JWT verification", options.purpose)
        assertEquals("sdjwt.domain.com", options.domain)
        assertEquals("sdjwt-challenge", options.challenge)
        assertTrue(options.sdjwt.contentEquals(arrayOf("ES256", "EdDSA")))
    }

    @Test
    fun testSDJWTPresentationOptionsEquality() {
        val options1 = SDJWTPresentationOptions(
            name = "Test",
            purpose = "Test purpose",
            sdjwt = arrayOf("EdDSA"),
            domain = "test.com",
            challenge = "challenge"
        )
        val options2 = SDJWTPresentationOptions(
            name = "Test",
            purpose = "Test purpose",
            sdjwt = arrayOf("EdDSA"),
            domain = "test.com",
            challenge = "challenge"
        )

        assertEquals(options1, options2)
        assertEquals(options1.hashCode(), options2.hashCode())
    }

    @Test
    fun testSDJWTPresentationOptionsInequalityDifferentSdjwt() {
        val options1 = SDJWTPresentationOptions(sdjwt = arrayOf("EdDSA"))
        val options2 = SDJWTPresentationOptions(sdjwt = arrayOf("ES256"))

        assertNotEquals(options1, options2)
    }

    @Test
    fun testSDJWTPresentationOptionsImplementsPresentationOptions() {
        val options: PresentationOptions = SDJWTPresentationOptions()

        assertEquals(CredentialType.SDJWT, options.type)
    }
}
