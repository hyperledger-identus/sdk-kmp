package org.hyperledger.identus.walletsdk.edgeagent

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hyperledger.identus.apollo.derivation.MnemonicHelper
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.castor.CastorImpl
import org.hyperledger.identus.walletsdk.domain.models.*
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.*
import org.hyperledger.identus.walletsdk.mercury.MercuryImpl
import org.hyperledger.identus.walletsdk.pluto.PlutoImpl
import org.hyperledger.identus.walletsdk.pluto.data.PlutoStorageImpl
import org.hyperledger.identus.walletsdk.pollux.PolluxImpl
import org.hyperledger.identus.walletsdk.pluto.DbConnectionInMemory
import org.hyperledger.identus.walletsdk.edgeagent.mediation.MediationHandler
import org.hyperledger.identus.walletsdk.logger.LoggerMock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hyperledger.identus.apollo.base64.base64UrlEncoded
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.OutOfBandInvitation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.ConnectionlessCredentialOffer
import kotlin.test.*
import java.util.UUID

class ConnectionlessIssuanceTest {

    private suspend fun createAgent(): EdgeAgent {
        val seed = Seed(MnemonicHelper.createRandomSeed())
        val apollo = ApolloImpl()
        val castor = CastorImpl(apollo)
        val pluto = PlutoImpl(PlutoStorageImpl(DbConnectionInMemory()), apollo)
        pluto.start()
        val pollux = PolluxImpl(apollo, castor)
        val mercury = MercuryImpl(castor, ApiImpl(httpClient()))
        val mediationHandler = MediationHandlerMock()
        val connectionManager = ConnectionManagerImpl(mercury, castor, pluto, mediationHandler, pollux = pollux)
        
        return EdgeAgent(
            apollo = apollo,
            castor = castor,
            pluto = pluto,
            mercury = mercury,
            pollux = pollux,
            connectionManager = connectionManager,
            seed = seed
        )
    }

    @Test
    fun testSuccessfulConnectionlessIssuance() = runTest {
        val issuer = createAgent()
        val holder = createAgent()

        // 1. Issuer creates a Prism DID for issuance
        val issuerDid = issuer.createNewPrismDID(emptyArray())

        // 2. Issuer creates an offer
        val credentialData = mapOf("name" to "Alice", "age" to "25")
        val preview = CredentialPreview(attributes = arrayOf(
            CredentialPreview.Attribute("name", "Alice"),
            CredentialPreview.Attribute("age", "25")
        ))
        
        val offer = OfferCredential.build(
            fromDID = issuerDid,
            toDID = DID("did:prism:placeholder"),
            thid = UUID.randomUUID().toString(),
            credentialPreview = preview,
            credentials = credentialData
        )

        // 3. Issuer wraps the offer in an Out-of-Band Invitation
        val offerMessage = offer.makeMessage()
        val offerMessageJson = Json.encodeToString(offerMessage)

        val attachment = AttachmentDescriptor(
            data = AttachmentData.AttachmentBase64(offerMessageJson.encodeToByteArray().base64UrlEncoded),
            mediaType = "application/json"
        )

        val invitation = OutOfBandInvitation(
            body = OutOfBandInvitation.Body(
                accept = listOf("didcomm/v2")
            ),
            from = issuerDid.toString(),
            attachments = arrayOf(attachment)
        )
        val invitationString = Json.encodeToString(invitation)

        // 4. Holder creates a Prism DID for receiving
        val holderDid = holder.createNewPrismDID(emptyArray())

        // 5. Holder parses the invitation
        val result = holder.parseInvitation(invitationString)
        assertTrue(result is ConnectionlessCredentialOffer, "Result should be a ConnectionlessCredentialOffer")
        
        val extractedOffer = (result as ConnectionlessCredentialOffer).offerCredential

        // 6. Holder prepares a request based on the extracted offer
        val request = holder.prepareRequestCredentialWithIssuer(holderDid, extractedOffer)

        // 7. Issuer processes the request and issues the credential
        val issue = IssueCredential.build(
            fromDID = issuerDid,
            toDID = holderDid,
            thid = request.thid,
            credentials = credentialData
        )

        // 8. Holder processes and stores the issued credential
        holder.processIssuedCredentialMessage(issue)

        // 9. Assertions
        val credentials = holder.getAllCredentials().first()
        assertTrue(credentials.isNotEmpty(), "Holder should have stored the credential")
        
        val didPairs = holder.getAllDIDPairs().first()
        assertTrue(didPairs.isEmpty(), "No DID pairs should be created in connectionless flow")
    }

    @Test
    fun testTamperedOffer() = runTest {
        val holder = createAgent()
        val holderDid = holder.createNewPrismDID(emptyArray())
        
        val preview = CredentialPreview(attributes = emptyArray())
        val offer = OfferCredential.build(
            fromDID = DID("did:prism:issuer"),
            toDID = DID("did:prism:placeholder"),
            thid = null, // Missing thid, should be rejected by prepareRequestCredentialWithIssuer
            credentialPreview = preview,
            credentials = mapOf("name" to "Alice")
        )

        assertFailsWith<EdgeAgentError.MissingOrNullFieldError> {
            holder.prepareRequestCredentialWithIssuer(holderDid, offer)
        }
    }

    @Test
    fun testMissingPrismDID() = runTest {
        val holder = createAgent()
        // Use a peer DID instead of prism DID for subject
        val holderDid = holder.createNewPeerDID(updateMediator = false)
        
        val preview = CredentialPreview(attributes = emptyArray())
        val offer = OfferCredential.build(
            fromDID = DID("did:prism:issuer"),
            toDID = DID("did:prism:placeholder"),
            thid = UUID.randomUUID().toString(),
            credentialPreview = preview,
            credentials = mapOf("name" to "Alice")
        )

        // Should fail because holder DID is not prism
        assertFailsWith<PolluxError.InvalidPrismDID> {
            holder.prepareRequestCredentialWithIssuer(holderDid, offer)
        }
    }
}
