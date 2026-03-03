package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for PresentationSubmissionOptions implementations:
 * - PresentationSubmissionOptionsJWT
 * - PresentationSubmissionOptionsAnoncreds
 */
class PresentationSubmissionOptionsTest {

    // PresentationSubmissionOptionsJWT tests
    @Test
    fun testPresentationSubmissionOptionsJWTCreation() {
        val options = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"input_descriptors":[]}"""
        )

        assertEquals("""{"input_descriptors":[]}""", options.presentationDefinitionRequest)
    }

    @Test
    fun testPresentationSubmissionOptionsJWTEquality() {
        val options1 = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"id":"test"}"""
        )
        val options2 = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"id":"test"}"""
        )

        assertEquals(options1, options2)
        assertEquals(options1.hashCode(), options2.hashCode())
    }

    @Test
    fun testPresentationSubmissionOptionsJWTInequality() {
        val options1 = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"id":"test1"}"""
        )
        val options2 = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"id":"test2"}"""
        )

        assertNotEquals(options1, options2)
    }

    @Test
    fun testPresentationSubmissionOptionsJWTImplementsInterface() {
        val options: PresentationSubmissionOptions = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = "{}"
        )

        assertTrue(options is PresentationSubmissionOptionsJWT)
    }

    // PresentationSubmissionOptionsAnoncreds tests
    @Test
    fun testPresentationSubmissionOptionsAnoncredsCreation() {
        val options = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"requested_attributes":{}}"""
        )

        assertEquals("""{"requested_attributes":{}}""", options.presentationDefinitionRequest)
    }

    @Test
    fun testPresentationSubmissionOptionsAnoncredsEquality() {
        val options1 = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"nonce":"123"}"""
        )
        val options2 = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"nonce":"123"}"""
        )

        assertEquals(options1, options2)
        assertEquals(options1.hashCode(), options2.hashCode())
    }

    @Test
    fun testPresentationSubmissionOptionsAnoncredsInequality() {
        val options1 = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"nonce":"123"}"""
        )
        val options2 = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"nonce":"456"}"""
        )

        assertNotEquals(options1, options2)
    }

    @Test
    fun testPresentationSubmissionOptionsAnoncredsImplementsInterface() {
        val options: PresentationSubmissionOptions = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = "{}"
        )

        assertTrue(options is PresentationSubmissionOptionsAnoncreds)
    }

    @Test
    fun testBothOptionsImplementSameInterface() {
        val jwtOptions: PresentationSubmissionOptions = PresentationSubmissionOptionsJWT(
            presentationDefinitionRequest = """{"type":"jwt"}"""
        )
        val anoncredsOptions: PresentationSubmissionOptions = PresentationSubmissionOptionsAnoncreds(
            presentationDefinitionRequest = """{"type":"anoncreds"}"""
        )

        assertTrue(jwtOptions is PresentationSubmissionOptions)
        assertTrue(anoncredsOptions is PresentationSubmissionOptions)
    }
}
