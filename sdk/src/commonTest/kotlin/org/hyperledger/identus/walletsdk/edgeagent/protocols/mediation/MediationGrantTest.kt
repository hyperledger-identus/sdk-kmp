package org.hyperledger.identus.walletsdk.edgeagent.protocols.mediation

import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.edgeagent.helpers.fromIndex
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Tests for MediationGrant following Coordinate Mediation Protocol 2.0
 * Protocol URI: https://didcomm.org/coordinate-mediation/2.0/mediate-grant
 */
class MediationGrantTest {

    @Test
    fun testMediationGrantCreation() {
        val routingDid = "did:peer:2.routing123"
        val body = MediationGrant.Body(routingDid = routingDid)

        val grant = MediationGrant(body = body)

        assertNotNull(grant.id)
        assertEquals(ProtocolType.DidcommMediationGrant.value, grant.type)
        assertEquals(routingDid, grant.body.routingDid)
    }

    @Test
    fun testMediationGrantWithCustomId() {
        val customId = "grant-123"
        val routingDid = "did:peer:2.routing456"
        val body = MediationGrant.Body(routingDid = routingDid)

        val grant = MediationGrant(id = customId, body = body)

        assertEquals(customId, grant.id)
        assertEquals(routingDid, grant.body.routingDid)
    }

    @Test
    fun testMediationGrantFromValidMessage() {
        val routingDid = "did:peer:2.routing789"
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommMediationGrant.value,
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            body = """{"routing_did":"$routingDid"}"""
        )

        val grant = MediationGrant(message)

        assertEquals("msg-123", grant.id)
        assertEquals(routingDid, grant.body.routingDid)
    }

    @Test
    fun testMediationGrantFromInvalidMessageTypeThrows() {
        // Using MediationRequest type instead of MediationGrant - should fail
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommMediationRequest.value,
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            body = """{"routing_did":"did:peer:2.test"}"""
        )

        assertFailsWith<MediationProtocolError.InvalidMediationGrantError> {
            MediationGrant(message)
        }
    }

    @Test
    fun testMediationGrantFromWrongProtocolThrows() {
        val message = Message(
            id = "msg-123",
            piuri = "https://wrong.protocol/type",
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            body = """{"routing_did":"did:peer:2.test"}"""
        )

        assertFailsWith<MediationProtocolError.InvalidMediationGrantError> {
            MediationGrant(message)
        }
    }

    @Test
    fun testMediationGrantTypeIsCorrectProtocol() {
        val grant = MediationGrant(
            body = MediationGrant.Body(routingDid = "did:peer:2.test")
        )

        assertEquals(
            "https://didcomm.org/coordinate-mediation/2.0/mediate-grant",
            grant.type
        )
    }

    @Test
    fun testMediationGrantBodySerialization() {
        val routingDid = "did:peer:2.serialization-test"
        val message = Message(
            id = "msg-456",
            piuri = ProtocolType.DidcommMediationGrant.value,
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            body = """{"routing_did":"$routingDid"}"""
        )

        val grant = MediationGrant(message)

        assertEquals(routingDid, grant.body.routingDid)
    }

    @Test
    fun testMediationGrantBodyDataClass() {
        val body1 = MediationGrant.Body(routingDid = "did:peer:2.same")
        val body2 = MediationGrant.Body(routingDid = "did:peer:2.same")
        val body3 = MediationGrant.Body(routingDid = "did:peer:2.different")

        assertEquals(body1, body2)
        assertEquals(body1.hashCode(), body2.hashCode())
        assert(body1 != body3)
    }
}
