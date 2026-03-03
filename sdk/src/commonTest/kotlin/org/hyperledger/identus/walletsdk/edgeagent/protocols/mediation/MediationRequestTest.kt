package org.hyperledger.identus.walletsdk.edgeagent.protocols.mediation

import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.edgeagent.helpers.fromIndex
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for MediationRequest following Coordinate Mediation Protocol 2.0
 * Protocol URI: https://didcomm.org/coordinate-mediation/2.0/mediate-request
 */
class MediationRequestTest {

    @Test
    fun testMediationRequestCreation() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)

        val request = MediationRequest(from = fromDID, to = toDID)

        assertEquals(fromDID, request.from)
        assertEquals(toDID, request.to)
        assertNotNull(request.id)
        assertEquals(ProtocolType.DidcommMediationRequest.value, request.type)
    }

    @Test
    fun testMediationRequestWithCustomId() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val customId = "custom-request-id"

        val request = MediationRequest(id = customId, from = fromDID, to = toDID)

        assertEquals(customId, request.id)
        assertEquals(fromDID, request.from)
        assertEquals(toDID, request.to)
    }

    @Test
    fun testMakeMessageReturnsValidMessage() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val request = MediationRequest(from = fromDID, to = toDID)

        val message = request.makeMessage()

        assertEquals(request.id, message.id)
        assertEquals(ProtocolType.DidcommMediationRequest.value, message.piuri)
        assertEquals(fromDID, message.from)
        assertEquals(toDID, message.to)
    }

    @Test
    fun testMakeMessageIncludesReturnRouteHeader() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val request = MediationRequest(from = fromDID, to = toDID)

        val message = request.makeMessage()

        // Coordinate Mediation 2.0 requires return_route header
        assertEquals("all", message.extraHeaders["return_route"])
    }

    @Test
    fun testMakeMessageHasEmptyBody() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val request = MediationRequest(from = fromDID, to = toDID)

        val message = request.makeMessage()

        // Mediation request has an empty body per spec
        assertEquals("{}", message.body)
    }

    @Test
    fun testMediationRequestTypeIsCorrectProtocol() {
        val request = MediationRequest(
            from = DID.fromIndex(0),
            to = DID.fromIndex(1)
        )

        assertEquals(
            "https://didcomm.org/coordinate-mediation/2.0/mediate-request",
            request.type
        )
    }

    @Test
    fun testMediationRequestEquality() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val id = "same-id"

        val request1 = MediationRequest(id = id, from = fromDID, to = toDID)
        val request2 = MediationRequest(id = id, from = fromDID, to = toDID)

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun testMediationRequestInequalityDifferentId() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)

        val request1 = MediationRequest(id = "id-1", from = fromDID, to = toDID)
        val request2 = MediationRequest(id = "id-2", from = fromDID, to = toDID)

        assertTrue(request1 != request2)
    }

    @Test
    fun testMediationRequestInequalityDifferentFrom() {
        val toDID = DID.fromIndex(index = 1)

        val request1 = MediationRequest(id = "same-id", from = DID.fromIndex(0), to = toDID)
        val request2 = MediationRequest(id = "same-id", from = DID.fromIndex(2), to = toDID)

        assertTrue(request1 != request2)
    }

    @Test
    fun testMediationRequestInequalityDifferentTo() {
        val fromDID = DID.fromIndex(index = 0)

        val request1 = MediationRequest(id = "same-id", from = fromDID, to = DID.fromIndex(1))
        val request2 = MediationRequest(id = "same-id", from = fromDID, to = DID.fromIndex(2))

        assertTrue(request1 != request2)
    }

    @Test
    fun testMakeMessageDirectionIsSent() {
        val request = MediationRequest(
            from = DID.fromIndex(0),
            to = DID.fromIndex(1)
        )

        val message = request.makeMessage()

        assertEquals(org.hyperledger.identus.walletsdk.domain.models.Message.Direction.SENT, message.direction)
    }
}
