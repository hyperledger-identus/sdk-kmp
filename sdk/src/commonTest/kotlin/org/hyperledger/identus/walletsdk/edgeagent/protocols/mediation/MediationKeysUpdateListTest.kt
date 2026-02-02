package org.hyperledger.identus.walletsdk.edgeagent.protocols.mediation

import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.edgeagent.ADD
import org.hyperledger.identus.walletsdk.edgeagent.helpers.fromIndex
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for MediationKeysUpdateList following Coordinate Mediation Protocol 2.0
 * Protocol URI: https://didcomm.org/coordinate-mediation/2.0/keylist-update
 */
class MediationKeysUpdateListTest {

    @Test
    fun testMediationKeysUpdateListCreation() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDids = arrayOf(DID.fromIndex(index = 2), DID.fromIndex(index = 3))

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        assertNotNull(updateList.id)
        assertEquals(fromDID, updateList.from)
        assertEquals(toDID, updateList.to)
        assertEquals(ProtocolType.DidcommMediationKeysUpdate.value, updateList.type)
        assertEquals(2, updateList.body.updates.size)
    }

    @Test
    fun testMediationKeysUpdateListWithCustomId() {
        val customId = "update-123"
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDids = arrayOf(DID.fromIndex(index = 2))

        val updateList = MediationKeysUpdateList(
            id = customId,
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        assertEquals(customId, updateList.id)
    }

    @Test
    fun testMediationKeysUpdateListUpdatesContainRecipientDids() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDid1 = DID.fromIndex(index = 2)
        val recipientDid2 = DID.fromIndex(index = 3)
        val recipientDids = arrayOf(recipientDid1, recipientDid2)

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        assertEquals(recipientDid1.toString(), updateList.body.updates[0].recipientDid)
        assertEquals(recipientDid2.toString(), updateList.body.updates[1].recipientDid)
    }

    @Test
    fun testMediationKeysUpdateListDefaultActionIsAdd() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDids = arrayOf(DID.fromIndex(index = 2))

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        assertEquals(ADD, updateList.body.updates[0].action)
    }

    @Test
    fun testMakeMessageReturnsValidMessage() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDids = arrayOf(DID.fromIndex(index = 2))

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        val message = updateList.makeMessage()

        assertEquals(updateList.id, message.id)
        assertEquals(ProtocolType.DidcommMediationKeysUpdate.value, message.piuri)
        assertEquals(fromDID, message.from)
        assertEquals(toDID, message.to)
    }

    @Test
    fun testMakeMessageBodyContainsUpdates() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDid = DID.fromIndex(index = 2)
        val recipientDids = arrayOf(recipientDid)

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        val message = updateList.makeMessage()

        assertTrue(message.body.contains("updates"))
        assertTrue(message.body.contains(recipientDid.toString()))
        assertTrue(message.body.contains(ADD))
    }

    @Test
    fun testMediationKeysUpdateListTypeIsCorrectProtocol() {
        val updateList = MediationKeysUpdateList(
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            recipientDids = arrayOf(DID.fromIndex(2))
        )

        assertEquals(
            "https://didcomm.org/coordinate-mediation/2.0/keylist-update",
            updateList.type
        )
    }

    @Test
    fun testMediationKeysUpdateListWithEmptyRecipientDids() {
        val fromDID = DID.fromIndex(index = 0)
        val toDID = DID.fromIndex(index = 1)
        val recipientDids = emptyArray<DID>()

        val updateList = MediationKeysUpdateList(
            from = fromDID,
            to = toDID,
            recipientDids = recipientDids
        )

        assertEquals(0, updateList.body.updates.size)
    }

    @Test
    fun testUpdateDataClass() {
        val update1 = MediationKeysUpdateList.Update(recipientDid = "did:test:1")
        val update2 = MediationKeysUpdateList.Update(recipientDid = "did:test:1")
        val update3 = MediationKeysUpdateList.Update(recipientDid = "did:test:2")

        assertEquals(update1, update2)
        assertEquals(update1.hashCode(), update2.hashCode())
        assert(update1 != update3)
    }

    @Test
    fun testUpdateDefaultAction() {
        val update = MediationKeysUpdateList.Update(recipientDid = "did:test:1")

        assertEquals(ADD, update.action)
    }

    @Test
    fun testBodyEquality() {
        val updates1 = arrayOf(
            MediationKeysUpdateList.Update(recipientDid = "did:test:1"),
            MediationKeysUpdateList.Update(recipientDid = "did:test:2")
        )
        val updates2 = arrayOf(
            MediationKeysUpdateList.Update(recipientDid = "did:test:1"),
            MediationKeysUpdateList.Update(recipientDid = "did:test:2")
        )
        val updates3 = arrayOf(
            MediationKeysUpdateList.Update(recipientDid = "did:test:3")
        )

        val body1 = MediationKeysUpdateList.Body(updates = updates1)
        val body2 = MediationKeysUpdateList.Body(updates = updates2)
        val body3 = MediationKeysUpdateList.Body(updates = updates3)

        assertEquals(body1, body2)
        assertEquals(body1.hashCode(), body2.hashCode())
        assert(body1 != body3)
    }

    @Test
    fun testBodyToMapStringAny() {
        val update = MediationKeysUpdateList.Update(recipientDid = "did:test:1")
        val body = MediationKeysUpdateList.Body(updates = arrayOf(update))

        val map = body.toMapStringAny()

        assertTrue(map.containsKey("updates"))
        @Suppress("UNCHECKED_CAST")
        val updates = map["updates"] as Array<MediationKeysUpdateList.Update>
        assertEquals(1, updates.size)
        assertEquals("did:test:1", updates[0].recipientDid)
    }

    @Test
    fun testMakeMessageDirectionIsSent() {
        val updateList = MediationKeysUpdateList(
            from = DID.fromIndex(0),
            to = DID.fromIndex(1),
            recipientDids = arrayOf(DID.fromIndex(2))
        )

        val message = updateList.makeMessage()

        assertEquals(
            org.hyperledger.identus.walletsdk.domain.models.Message.Direction.SENT,
            message.direction
        )
    }
}
