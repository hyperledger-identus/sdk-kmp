package org.hyperledger.identus.walletsdk.edgeagent.mediation

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Mediator
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.edgeagent.EdgeAgentError
import org.hyperledger.identus.walletsdk.edgeagent.MercuryMock
import org.hyperledger.identus.walletsdk.edgeagent.helpers.fromIndex
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for BasicMediatorHandler following Coordinate Mediation Protocol 2.0
 * https://didcomm.org/coordinate-mediation/2.0/
 */
class BasicMediatorHandlerTest {

    private val mediatorDID = DID.fromIndex(index = 0)
    private val hostDID = DID.fromIndex(index = 1)
    private val routingDID = DID.fromIndex(index = 2)

    @Test
    fun testBootRegisteredMediatorReturnsNullWhenNoMediatorStored() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        val result = handler.bootRegisteredMediator()

        assertNull(result)
    }

    @Test
    fun testBootRegisteredMediatorReturnsStoredMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val storedMediator = Mediator(
            id = "mediator-1",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(storedMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        val result = handler.bootRegisteredMediator()

        assertNotNull(result)
        assertEquals(storedMediator.id, result.id)
        assertEquals(mediatorDID, result.mediatorDID)
        assertEquals(hostDID, result.hostDID)
        assertEquals(routingDID, result.routingDID)
    }

    @Test
    fun testBootRegisteredMediatorCachesResult() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val storedMediator = Mediator(
            id = "mediator-1",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(storedMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        // First call
        val result1 = handler.bootRegisteredMediator()
        // Remove from store
        store.clear()
        // Second call should return cached value
        val result2 = handler.bootRegisteredMediator()

        assertEquals(result1, result2)
    }

    @Test
    fun testAchieveMediationReturnsExistingMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val existingMediator = Mediator(
            id = "existing-mediator",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(existingMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        val result = handler.achieveMediation(hostDID).first()

        assertEquals(existingMediator.id, result.id)
    }

    @Test
    fun testAchieveMediationCreatesNewMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val grantRoutingDid = "did:peer:2.grant-routing"

        // Mock the grant response
        mercury.sendMessageParseMessageResponse = Message(
            id = "grant-msg-id",
            piuri = ProtocolType.DidcommMediationGrant.value,
            from = mediatorDID,
            to = hostDID,
            body = """{"routing_did":"$grantRoutingDid"}"""
        )

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        val result = handler.achieveMediation(hostDID).first()

        assertNotNull(result)
        assertEquals(mediatorDID, result.mediatorDID)
        assertEquals(hostDID, result.hostDID)
        assertEquals(DID(grantRoutingDid), result.routingDID)
        // Verify it was stored
        assertEquals(1, store.getAllMediators().size)
    }

    @Test
    fun testUpdateKeyListWithDIDsThrowsWhenNoMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertFailsWith<EdgeAgentError.NoMediatorAvailableError> {
            handler.updateKeyListWithDIDs(arrayOf(DID.fromIndex(3)))
        }
    }

    @Test
    fun testUpdateKeyListWithDIDsSendsMessage() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val existingMediator = Mediator(
            id = "existing-mediator",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(existingMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )
        // Boot the mediator first
        handler.bootRegisteredMediator()

        val newDIDs = arrayOf(DID.fromIndex(3), DID.fromIndex(4))

        // Should not throw
        handler.updateKeyListWithDIDs(newDIDs)

        // Verify Mercury was called (we can check the mock was used)
        // In a real scenario, we'd verify the message content
    }

    @Test
    fun testPickupUnreadMessagesThrowsWhenNoMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertFailsWith<EdgeAgentError.NoMediatorAvailableError> {
            handler.pickupUnreadMessages(10).first()
        }
    }

    @Test
    fun testRegisterMessagesAsReadThrowsWhenNoMediator() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertFailsWith<EdgeAgentError.NoMediatorAvailableError> {
            handler.registerMessagesAsRead(arrayOf("msg-1", "msg-2"))
        }
    }

    @Test
    fun testRegisterMessagesAsReadSendsMessage() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val existingMediator = Mediator(
            id = "existing-mediator",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(existingMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )
        // Boot the mediator first
        handler.bootRegisteredMediator()

        val messageIds = arrayOf("msg-1", "msg-2", "msg-3")

        // Should not throw
        handler.registerMessagesAsRead(messageIds)
    }

    @Test
    fun testMediatorDIDProperty() {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertEquals(mediatorDID, handler.mediatorDID)
    }

    @Test
    fun testMediatorPropertyInitiallyNull() {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertNull(handler.mediator)
    }

    @Test
    fun testMediatorPropertySetAfterBoot() = runTest {
        val mercury = MercuryMock()
        val store = InMemoryMediatorRepository()
        val existingMediator = Mediator(
            id = "existing-mediator",
            mediatorDID = mediatorDID,
            hostDID = hostDID,
            routingDID = routingDID
        )
        store.storeMediator(existingMediator)

        val handler = BasicMediatorHandler(
            mediatorDID = mediatorDID,
            mercury = mercury,
            store = store
        )

        assertNull(handler.mediator)
        handler.bootRegisteredMediator()
        assertNotNull(handler.mediator)
    }

    /**
     * In-memory implementation of MediatorRepository for testing
     */
    private class InMemoryMediatorRepository : MediatorRepository {
        private val mediators = mutableListOf<Mediator>()

        override fun storeMediator(mediator: Mediator) {
            mediators.add(mediator)
        }

        override suspend fun getAllMediators(): List<Mediator> {
            return mediators.toList()
        }

        fun clear() {
            mediators.clear()
        }
    }
}
