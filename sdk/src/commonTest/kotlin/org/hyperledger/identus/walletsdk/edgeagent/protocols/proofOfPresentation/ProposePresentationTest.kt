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
 * Tests for ProposePresentation following Present Proof Protocol 3.0
 * Protocol URI: https://atalaprism.io/present-proof/3.0/propose-presentation
 * (Identus-specific implementation based on DIF Present Proof)
 */
class ProposePresentationTest {

    private val fromDID = DID.fromIndex(index = 0)
    private val toDID = DID.fromIndex(index = 1)

    @Test
    fun testProposePresentationCreation() {
        val body = ProposePresentation.Body(
            goalCode = "propose-proof",
            comment = "I can provide these proofs",
            proofTypes = arrayOf(
                ProofTypes(schema = "https://example.com/schema/1.0")
            )
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal = ProposePresentation(
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        assertNotNull(proposal.id)
        assertEquals(body, proposal.body)
        assertEquals(fromDID, proposal.from)
        assertEquals(toDID, proposal.to)
        assertEquals(ProtocolType.DidcommProposePresentation.value, proposal.type)
    }

    @Test
    fun testProposePresentationWithCustomId() {
        val customId = "proposal-123"
        val body = ProposePresentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal = ProposePresentation(
            id = customId,
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        assertEquals(customId, proposal.id)
    }

    @Test
    fun testProposePresentationWithThreadId() {
        val body = ProposePresentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()
        val threadId = "thread-456"

        val proposal = ProposePresentation(
            body = body,
            attachments = attachments,
            thid = threadId,
            from = fromDID,
            to = toDID
        )

        assertEquals(threadId, proposal.thid)
    }

    @Test
    fun testMakeMessageReturnsValidMessage() {
        val body = ProposePresentation.Body(
            goalCode = "offer-credentials",
            comment = "I can provide these"
        )
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal = ProposePresentation(
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        val message = proposal.makeMessage()

        assertEquals(proposal.id, message.id)
        assertEquals(ProtocolType.DidcommProposePresentation.value, message.piuri)
        assertEquals(fromDID, message.from)
        assertEquals(toDID, message.to)
    }

    @Test
    fun testMakeMessageBodyContainsGoalCode() {
        val goalCode = "propose-age-proof"
        val body = ProposePresentation.Body(goalCode = goalCode)
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal = ProposePresentation(
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        val message = proposal.makeMessage()

        assertTrue(message.body.contains("goal_code"))
        assertTrue(message.body.contains(goalCode))
    }

    // NOTE: The ProposePresentation(fromMessage: Message) constructor has a bug -
    // it creates a new ProposePresentation object instead of initializing `this`.
    // This test verifies it throws for invalid input but does not test correct parsing.
    @Test
    fun testConstructorFromMessageThrowsForInvalidType() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommPresentation.value,
            from = fromDID,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            ProposePresentation(message)
        }
    }

    @Test
    fun testConstructorFromMessageThrowsForMissingFrom() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommProposePresentation.value,
            from = null,
            to = toDID,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            ProposePresentation(message)
        }
    }

    @Test
    fun testConstructorFromMessageThrowsForMissingTo() {
        val message = Message(
            id = "msg-123",
            piuri = ProtocolType.DidcommProposePresentation.value,
            from = fromDID,
            to = null,
            body = "{}"
        )

        assertFailsWith<EdgeAgentError.InvalidMessageType> {
            ProposePresentation(message)
        }
    }

    @Test
    fun testProposePresentationTypeIsCorrectProtocol() {
        val proposal = ProposePresentation(
            body = ProposePresentation.Body(),
            attachments = emptyArray(),
            thid = null,
            from = fromDID,
            to = toDID
        )

        // Identus-specific protocol URI (legacy from Atala Prism)
        assertEquals(
            "https://didcomm.atalaprism.io/present-proof/3.0/propose-presentation",
            proposal.type
        )
    }

    @Test
    fun testProposePresentationEquality() {
        val id = "same-id"
        val body = ProposePresentation.Body(goalCode = "test")
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal1 = ProposePresentation(
            id = id,
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )
        val proposal2 = ProposePresentation(
            id = id,
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        assertEquals(proposal1, proposal2)
        assertEquals(proposal1.hashCode(), proposal2.hashCode())
    }

    @Test
    fun testProposePresentationInequalityDifferentId() {
        val body = ProposePresentation.Body()
        val attachments = emptyArray<AttachmentDescriptor>()

        val proposal1 = ProposePresentation(
            id = "id-1",
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )
        val proposal2 = ProposePresentation(
            id = "id-2",
            body = body,
            attachments = attachments,
            thid = null,
            from = fromDID,
            to = toDID
        )

        assertTrue(proposal1 != proposal2)
    }

    @Test
    fun testBodyEquality() {
        val proofTypes = arrayOf(ProofTypes(schema = "test-schema"))

        val body1 = ProposePresentation.Body(
            goalCode = "code",
            comment = "comment",
            proofTypes = proofTypes
        )
        val body2 = ProposePresentation.Body(
            goalCode = "code",
            comment = "comment",
            proofTypes = proofTypes
        )
        val body3 = ProposePresentation.Body(
            goalCode = "different",
            comment = "comment",
            proofTypes = proofTypes
        )

        assertEquals(body1, body2)
        assertEquals(body1.hashCode(), body2.hashCode())
        assertTrue(body1 != body3)
    }

    @Test
    fun testBodyWithNullValues() {
        val body = ProposePresentation.Body()

        assertEquals(null, body.goalCode)
        assertEquals(null, body.comment)
    }
}
