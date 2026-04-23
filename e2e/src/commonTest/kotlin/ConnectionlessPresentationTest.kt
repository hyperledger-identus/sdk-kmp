package org.hyperledger.identus.walletsdk.edgeagent

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.castor.CastorImpl
import org.hyperledger.identus.walletsdk.domain.models.Api
import org.hyperledger.identus.walletsdk.domain.models.AttachmentBase64
import org.hyperledger.identus.walletsdk.domain.models.AttachmentDescriptor
import org.hyperledger.identus.walletsdk.domain.models.CredentialType
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Mediator
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.domain.models.PresentationClaims
import org.hyperledger.identus.walletsdk.edgeagent.mediation.MediationHandler
import org.hyperledger.identus.walletsdk.edgeagent.mediation.OnMessageCallback
import org.hyperledger.identus.walletsdk.edgeagent.protocols.ProtocolType
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.ConnectionlessRequestPresentation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.OutOfBandInvitation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation.Presentation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation.RequestPresentation
import org.hyperledger.identus.walletsdk.mercury.MercuryImpl
import org.hyperledger.identus.walletsdk.mercury.resolvers.DIDCommWrapper
import org.hyperledger.identus.walletsdk.pluto.PlutoImpl
import org.hyperledger.identus.walletsdk.pluto.data.DbConnection
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.hyperledger.identus.walletsdk.SdkPlutoDb
import org.hyperledger.identus.walletsdk.pollux.PolluxImpl
import org.hyperledger.identus.apollo.base64.base64UrlEncoded
import org.hyperledger.identus.walletsdk.pollux.models.JWTCredential
import org.hyperledger.identus.walletsdk.domain.models.ApiImpl
import org.hyperledger.identus.walletsdk.domain.models.httpClient
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * E2E tests for Connectionless Presentation flow.
 * Validates the flow from Verifier request to Holder response and Verifier verification.
 */
class ConnectionlessPresentationTest {

    private lateinit var verifier: EdgeAgent
    private lateinit var holder: EdgeAgent
    private val apollo = ApolloImpl()
    private val castor = CastorImpl(apollo)

    // Using a fake database connection for tests
    class DbConnectionInMemory : DbConnection {
        override var driver: SqlDriver? = null
        override suspend fun connectDb(context: Any?): SqlDriver {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            SdkPlutoDb.Schema.create(driver)
            this.driver = driver
            return driver
        }
    }

    // Fake mediation handler to allow ConnectionManagerImpl to work without real network
    class FakeMediationHandler(override val mediatorDID: DID = DID("did:peer:mediator")) : MediationHandler {
        override val mediator: Mediator = Mediator(mediatorDID, DID("did:peer:host"), DID("did:peer:routing"))
        override suspend fun bootRegisteredMediator() = mediator
        override fun achieveMediation(host: DID) = flowOf(mediator)
        override suspend fun updateKeyListWithDIDs(dids: Array<DID>) {}
        override fun pickupUnreadMessages(limit: Int) = emptyFlow()
        override suspend fun registerMessagesAsRead(ids: Array<String>) {}
        override suspend fun listenUnreadMessages(serviceEndpointUri: String, onMessageCallback: OnMessageCallback) {}
    }

    @BeforeTest
    fun setup() = runTest {
        val api = ApiImpl(httpClient())
        
        // Initialize Verifier
        val verifierPluto = PlutoImpl(DbConnectionInMemory())
        verifierPluto.start()
        val verifierMercury = MercuryImpl(castor, DIDCommWrapper(castor, verifierPluto, apollo), api)
        verifier = EdgeAgent(
            apollo = apollo,
            castor = castor,
            pluto = verifierPluto,
            mercury = verifierMercury,
            pollux = PolluxImpl(apollo, castor),
            seed = apollo.createRandomSeed().seed,
            mediatorHandler = FakeMediationHandler()
        )

        // Initialize Holder
        val holderPluto = PlutoImpl(DbConnectionInMemory())
        holderPluto.start()
        val holderMercury = MercuryImpl(castor, DIDCommWrapper(castor, holderPluto, apollo), api)
        holder = EdgeAgent(
            apollo = apollo,
            castor = castor,
            pluto = holderPluto,
            mercury = holderMercury,
            pollux = PolluxImpl(apollo, castor),
            seed = apollo.createRandomSeed().seed,
            mediatorHandler = FakeMediationHandler()
        )
    }

    @Test
    fun testSuccessfulConnectionlessPresentation() = runTest {
        // 1. Verifier creates a presentation request
        val claims = PresentationClaims(
            "test-schema",
            arrayOf(PresentationClaims.Claim("name", "string"))
        )
        
        val targetDID = DID("did:peer:target")
        verifier.initiatePresentationRequest(
            type = CredentialType.JWT,
            toDID = targetDID,
            presentationClaims = claims,
            domain = "test-domain",
            challenge = "test-challenge"
        )

        // Retrieve the request from Pluto (SDK stores it during initiatePresentationRequest)
        val sentMsg = verifier.pluto.getAllMessagesSentTo(targetDID).first().first()
        val request = RequestPresentation.fromMessage(sentMsg)

        // 3. Create OOB invitation with request as attachment
        val invitation = OutOfBandInvitation(
            body = OutOfBandInvitation.Body(accept = listOf("didcomm/v2")),
            from = verifier.createNewPeerDID(updateMediator = false).toString(),
            attachments = arrayOf(
                AttachmentDescriptor(
                    id = "request-presentation",
                    data = AttachmentBase64(request.makeMessage().body.encodeToByteArray().base64UrlEncoded),
                    format = ProtocolType.DidcommRequestPresentation.value
                )
            )
        )

        // 4. Convert invitation to string
        val invitationString = invitation.toString()

        // 5. Holder processes invitation
        val result = holder.parseInvitation(invitationString)

        // 6. ASSERT TYPE
        assertTrue(result is ConnectionlessRequestPresentation)

        // 7. Extract request
        val extractedRequest = (result as ConnectionlessRequestPresentation).requestPresentation

        // 8. ASSERT connectionless: No DIDPairs should be created
        val didPairs = holder.pluto.getAllDidPairs().first()
        assertTrue(didPairs.isEmpty(), "Connectionless flow should not create DID pairs")

        // 9. ASSERT message stored in Pluto
        val messages = holder.pluto.getAllMessages().first()
        assertTrue(messages.any { it.id == extractedRequest.id }, "Message should be stored in Pluto")

        // 10. Holder creates presentation
        // Simple dummy credential for the test
        val credentialJwt = "eyJhbGciOiJFUzI1NksifQ.eyJpc3MiOiJkaWQ6cHJpc206dGVzdCIsInN1YiI6ImRpZDpwcmlzbTpob2xkZXIiLCJ2YyI6eyJjcmVkZW50aWFsU3ViamVjdCI6eyJuYW1lIjoiQWxpY2UifX19.sig"
        val credential = JWTCredential.fromJwtString(credentialJwt)

        val presentation = holder.preparePresentationForRequestProof(
            request = extractedRequest,
            credential = credential
        )

        // 11. Verify presentation
        val isValid = verifier.handlePresentation(presentation.makeMessage())

        // 12. ASSERT valid
        assertTrue(isValid, "Presentation verification should succeed for valid connectionless flow")
    }

    @Test
    fun testTamperedRequestFails() = runTest {
        // 1. Create valid request
        val claims = PresentationClaims("test", arrayOf(PresentationClaims.Claim("name", "string")))
        verifier.initiatePresentationRequest(type = CredentialType.JWT, toDID = DID("did:peer:test"), presentationClaims = claims)
        val originalMsg = verifier.pluto.getAllMessagesSentTo(DID("did:peer:test")).first().first()
        val request = RequestPresentation.fromMessage(originalMsg)

        // 2. Tamper: Change goalCode or thid
        val tamperedBody = request.body.copy(goalCode = "tampered-goal")
        val tamperedRequest = request.copy(body = tamperedBody)

        // 3. Generate presentation based on tampered request
        val credential = JWTCredential.fromJwtString("eyJhbGciOiJFUzI1NksifQ.eyJpc3MiOiJpc3N1ZXIifQ.sig")
        val presentation = holder.preparePresentationForRequestProof(tamperedRequest, credential)

        // 4. Verify
        val isValid = verifier.handlePresentation(presentation.makeMessage())

        // 5. ASSERT falsy because the request in Verifier's Pluto doesn't match the presentation's thid/content
        assertFalse(isValid)
    }

    @Test
    fun testMissingCredentialsFails() = runTest {
        // 1. Request credential type not available
        val request = RequestPresentation(
            from = DID("did:peer:verifier"),
            to = DID("did:peer:holder"),
            body = RequestPresentation.Body(),
            attachments = arrayOf(
                AttachmentDescriptor(
                    id = "req-1",
                    format = "UnsupportedType",
                    data = AttachmentBase64("{}".encodeToByteArray().base64UrlEncoded)
                )
            )
        )

        // 2. Call and 3. ASSERT
        assertFailsWith<Exception> {
            holder.preparePresentationForRequestProof(
                request = request,
                credential = JWTCredential.fromJwtString("empty")
            )
        }
    }
}
