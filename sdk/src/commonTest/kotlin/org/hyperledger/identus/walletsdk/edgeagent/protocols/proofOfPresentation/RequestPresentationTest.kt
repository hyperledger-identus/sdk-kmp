package org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation

import org.hyperledger.identus.walletsdk.domain.models.AttachmentDescriptor
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgentError
import org.hyperledger.identus.walletsdk.edgeagent.helpers.fromIndex
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for RequestPresentation following Present Proof Protocol 3.0
 * Protocol URI: https://atalaprism.io/present-proof/3.0/request-presentation
 * (Identus-specific implementation based on DIF Present Proof)
 */
class RequestPresentationTest {

    private val fromDID = DID.fromIndex(index = 0)
    private val toDID = DID.fromIndex(index = 1)

    @Test
    fun testRequestPresentationCreation() {
        val body = RequestPresentation.Body(
            goalCode = "request-proof",
            comment = "Please provide proof",
            willConfirm = true,
            proofTypes = arrayOf(
                ProofTypes(schema = "https://example.com/schema/1.0")
            )
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val request = RequestPresentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertNotNull(request.id)
        assertEquals(body, request.body)
        assertEquals(fromDID, request.from)
        assertEquals(toDID, request.to)
        assertEquals(ProtocolType.DidcommRequestPresentation.value, request.type)
    }

    @Test
    fun testRequestPresentationWithCustomId() {
        val customId = "request-123"
        val body = RequestPresentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()

        val request = RequestPresentation(
            id = customId,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertEquals(customId, request.id)
    }

    @Test
    fun testRequestPresentationWithThreadId() {
        val body = RequestPresentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()
        val threadId = "thread-456"

        val request = RequestPresentation(
            body = body,
            attachments = attachments,
            thid = threadId,
            from = fromDID,
            to = toDID
        )

        assertEquals(threadId, request.thid)
    }

    @Test
    fun testMakeMessageReturnsValidMessage() {
        val body = RequestPresentation.Body(
            goalCode = "verify-identity",
            comment = "Identity verification"
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val request = RequestPresentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        val message = request.makeMessage()

        assertEquals(request.id, message.id)
        assertEquals(ProtocolType.DidcommRequestPresentation.value, message.piuri)
        assertEquals(fromDID, message.from)
        assertEquals(toDID, message.to)
    }

    @Test
    fun testMakeMessageBodyContainsGoalCode() {
        val goalCode = "verify-age"
        val body = RequestPresentation.Body(goalCode = goalCode)
        val attachments = emptyArray<AttachmentDescriptor>()

        val request = RequestPresentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        val message = request.makeMessage()

        assertTrue(message.body.contains("goal_code"))
        assertTrue(message.body.contains(goalCode))
    }

    @Test
    fun testFromMessageWithValidMessage() {
        val body = RequestPresentation.Body(
            goalCode = "test-goal",
            comment = "Test comment",
            willConfirm = true,
            proofTypes = arrayOf(ProofTypes(schema = "test-schema"))
        )
        val original = RequestPresentation(
            body = body,
            attachments = emptyArray(),
            from = fromDID,
            to = toDID
        )
        val message = original.makeMessage()

        val parsed = RequestPresentation.fromMessage(message)

        assertEquals(original.id, parsed.id)
        assertEquals(original.body.goalCode, parsed.body.goalCode)
        assertEquals(original.body.comment, parsed.body.comment)
        assertEquals(original.from, parsed.from)
        assertEquals(original.to, parsed.to)
    }

    @Test
    fun testFromMessageWithInvalidTypeThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommPresentation.value,
            from = fromDID,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            RequestPresentation.fromMessage(message)
        }
    }

    @Test
    fun testFromMessageWithMissingFromThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommRequestPresentation.value,
            from = null,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            RequestPresentation.fromMessage(message)
        }
    }

    @Test
    fun testFromMessageWithMissingToThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommRequestPresentation.value,
            from = fromDID,
            to = null,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            RequestPresentation.fromMessage(message)
        }
    }

    @Test
    fun testRequestPresentationTypeIsCorrectProtocol() {
        val request = RequestPresentation(
            body = RequestPresentation.Body(),
            attachments = emptyArray(),
            from = fromDID,
            to = toDID
        )

        // Identus-specific protocol URI (legacy from Atala Prism)
        assertEquals(
            "https://didcomm.atalaprism.io/present-proof/3.0/request-presentation",
            request.type
        )
    }

    @Test
    fun testRequestPresentationEquality() {
        val id = "same-id"
        val body = RequestPresentation.Body(goalCode = "test")
        val attachments = emptyArray<AttachmentDescriptor>()

        val request1 = RequestPresentation(
            id = id,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )
        val request2 = RequestPresentation(
            id = id,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun testBodyEquality() {
        val proofTypes = arrayOf(ProofTypes(schema = "test-schema"))

        val body1 = RequestPresentation.Body(
            goalCode = "code",
            comment = "comment",
            willConfirm = true,
            proofTypes = proofTypes
        )
        val body2 = RequestPresentation.Body(
            goalCode = "code",
            comment = "comment",
            willConfirm = true,
            proofTypes = proofTypes
        )
        val body3 = RequestPresentation.Body(
            goalCode = "different",
            comment = "comment",
            willConfirm = true,
            proofTypes = proofTypes
        )

        assertEquals(body1, body2)
        assertEquals(body1.hashCode(), body2.hashCode())
        assertTrue(body1 != body3)
    }

    @Test
    fun testDirectionDefaultsToReceived() {
        val request = RequestPresentation(
            body = RequestPresentation.Body(),
            attachments = emptyArray(),
            from = fromDID,
            to = toDID
        )

        assertEquals(Message.Direction.RECEIVED, request.direction)
    }

    @Test
    fun testMakeMessageIncludesDirection() {
        val request = RequestPresentation(
            body = RequestPresentation.Body(),
            attachments = emptyArray(),
            from = fromDID,
            to = toDID,
            direction = Message.Direction.SENT
        )

        val message = request.makeMessage()

        assertEquals(Message.Direction.SENT, message.direction)
    }
}
