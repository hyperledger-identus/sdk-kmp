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
 * Tests for Presentation following Present Proof Protocol 3.0
 * Protocol URI: https://atalaprism.io/present-proof/3.0/presentation
 * (Identus-specific implementation based on DIF Present Proof)
 */
class PresentationTest {

    private val fromDID = DID.fromIndex(index = 0)
    private val toDID = DID.fromIndex(index = 1)

    @Test
    fun testPresentationCreation() {
        val body = Presentation.Body(
            goalCode = "present-credentials",
            comment = "Here are my credentials"
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation = Presentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertNotNull(presentation.id)
        assertEquals(body, presentation.body)
        assertEquals(fromDID, presentation.from)
        assertEquals(toDID, presentation.to)
        assertEquals(ProtocolType.DidcommPresentation.value, presentation.type)
    }

    @Test
    fun testPresentationWithCustomId() {
        val customId = "presentation-123"
        val body = Presentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation = Presentation(
            id = customId,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertEquals(customId, presentation.id)
    }

    @Test
    fun testPresentationWithThreadId() {
        val body = Presentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()
        val threadId = "thread-789"

        val presentation = Presentation(
            body = body,
            attachments = attachments,
            thid = threadId,
            from = fromDID,
            to = toDID
        )

        assertEquals(threadId, presentation.thid)
    }

    @Test
    fun testMakeMessageReturnsValidMessage() {
        val body = Presentation.Body(
            goalCode = "identity-proof",
            comment = "Providing identity proof"
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation = Presentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        val message = presentation.makeMessage()

        assertEquals(presentation.id, message.id)
        assertEquals(ProtocolType.DidcommPresentation.value, message.piuri)
        assertEquals(fromDID, message.from)
        assertEquals(toDID, message.to)
    }

    @Test
    fun testMakeMessageBodyContainsGoalCode() {
        val goalCode = "provide-age-proof"
        val body = Presentation.Body(goalCode = goalCode)
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation = Presentation(
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        val message = presentation.makeMessage()

        assertTrue(message.body.contains("goalCode"))
        assertTrue(message.body.contains(goalCode))
    }

    @Test
    fun testFromMessageStaticMethodWithValidMessage() {
        val body = Presentation.Body(
            goalCode = "test-goal",
            comment = "Test comment"
        )
        val original = Presentation(
            body = body,
            attachments = emptyArray(),
            from = fromDID,
            to = toDID
        )
        val message = original.makeMessage()

        val parsed = Presentation.fromMessage(message)

        assertEquals(original.id, parsed.id)
        assertEquals(original.body.goalCode, parsed.body.goalCode)
        assertEquals(original.body.comment, parsed.body.comment)
        assertEquals(original.from, parsed.from)
        assertEquals(original.to, parsed.to)
    }

    @Test
    fun testFromMessageStaticMethodWithInvalidTypeThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommRequestPresentation.value,
            from = fromDID,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            Presentation.fromMessage(message)
        }
    }

    @Test
    fun testFromMessageStaticMethodWithMissingFromThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommPresentation.value,
            from = null,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            Presentation.fromMessage(message)
        }
    }

    @Test
    fun testFromMessageStaticMethodWithMissingToThrows() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommPresentation.value,
            from = fromDID,
            to = null,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            Presentation.fromMessage(message)
        }
    }

    // NOTE: The Presentation(fromMessage: Message) constructor has a bug -
    // it creates a new Presentation object instead of initializing `this`.
    // This test verifies it throws for invalid input but does not test correct parsing.
    @Test
    fun testConstructorFromMessageThrowsForInvalidType() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommRequestPresentation.value,
            from = fromDID,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            Presentation(message)
        }
    }

    @Test
    fun testPresentationTypeIsCorrectProtocol() {
        val presentation = Presentation(
            body = Presentation.Body(),
            attachments = emptyArray(),
            from = fromDID,
            to = toDID
        )

        // Identus-specific protocol URI (legacy from Atala Prism)
        assertEquals(
            "https://didcomm.atalaprism.io/present-proof/3.0/presentation",
            presentation.type
        )
    }

    @Test
    fun testPresentationEquality() {
        val id = "same-id"
        val body = Presentation.Body(goalCode = "test")
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation1 = Presentation(
            id = id,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )
        val presentation2 = Presentation(
            id = id,
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertEquals(presentation1, presentation2)
        assertEquals(presentation1.hashCode(), presentation2.hashCode())
    }

    @Test
    fun testPresentationInequalityDifferentId() {
        val body = Presentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()

        val presentation1 = Presentation(
            id = "id-1",
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )
        val presentation2 = Presentation(
            id = "id-2",
            body = body,
            attachments = attachments,
            from = fromDID,
            to = toDID
        )

        assertTrue(presentation1 != presentation2)
    }

    @Test
    fun testBodyEquality() {
        val body1 = Presentation.Body(goalCode = "code", comment = "comment")
        val body2 = Presentation.Body(goalCode = "code", comment = "comment")
        val body3 = Presentation.Body(goalCode = "different", comment = "comment")

        assertEquals(body1, body2)
        assertEquals(body1.hashCode(), body2.hashCode())
        assertTrue(body1 != body3)
    }

    @Test
    fun testBodyWithNullValues() {
        val body = Presentation.Body()

        assertEquals(null, body.goalCode)
        assertEquals(null, body.comment)
    }
}
