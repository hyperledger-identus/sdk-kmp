package org.hyperledger.identus.walletsdk.workflow

import com.google.gson.JsonParser
import io.iohk.atala.automation.extensions.body
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.matchers.RestAssuredJsonProperty
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.serenity.interactions.PollingWait
import io.iohk.atala.automation.serenity.questions.HttpRequest
import io.iohk.atala.automation.utils.Wait
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.rest.SerenityRest.lastResponse
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Patch
import net.serenitybdd.screenplay.rest.interactions.Post
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.hyperledger.identus.client.models.AnoncredPresentationRequestV1
import org.hyperledger.identus.client.models.AnoncredRequestedAttributeV1
import org.hyperledger.identus.client.models.AnoncredRequestedPredicateV1
import org.hyperledger.identus.client.models.CreateConnectionRequest
import org.hyperledger.identus.client.models.CreateIssueCredentialRecordRequest
import org.hyperledger.identus.client.models.Options
import org.hyperledger.identus.client.models.ProofRequestAux
import org.hyperledger.identus.client.models.RequestPresentationInput
import org.hyperledger.identus.walletsdk.configuration.Environment
import org.hyperledger.identus.walletsdk.utils.Utils
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

object CloudAgentWorkflow {

    fun hasNoConnection(cloudAgent: Actor, edgeAgent: Actor) {
        assertThat(cloudAgent.recall<String>("connectionId")).isNull()
        assertThat(edgeAgent.recall<String>("connectionId")).isNull()
    }

    fun createConnection(cloudAgent: Actor, label: String? = null, goalCode: String? = null, goal: String? = null) {
        val createConnection = CreateConnectionRequest(label, goalCode, goal)

        cloudAgent.attemptsTo(
            Post.to("/connections").body(createConnection),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )

        cloudAgent.remember("invitation", lastResponse().get<String>("invitation.invitationUrl"))
        cloudAgent.remember("connectionId", lastResponse().get<String>("connectionId"))
    }

    fun shareInvitation(cloudAgent: Actor, edgeAgent: Actor) {
        val invitation = cloudAgent.recall<String>("invitation")
        edgeAgent.remember("invitation", invitation)
    }

    fun waitForConnectionState(cloudAgent: Actor, state: String) {
        val connectionId = cloudAgent.recall<String>("connectionId")
        cloudAgent.attemptsTo(
            PollingWait.until(
                HttpRequest.get("/connections/$connectionId"),
                RestAssuredJsonProperty.toBe("state", state)
            )
        )
    }

    fun verifyCredentialState(cloudAgent: Actor, recordId: String, state: String) {
        cloudAgent.attemptsTo(
            PollingWait.until(
                HttpRequest.get("/issue-credentials/records/$recordId"),
                RestAssuredJsonProperty.toBe("protocolState", state)
            )
        )
    }

    fun verifyPresentProof(cloudAgent: Actor, state: String) {
        val presentationId = cloudAgent.recall<String>("presentationId")
        cloudAgent.attemptsTo(
            PollingWait.until(
                HttpRequest.get("/present-proof/presentations/$presentationId"),
                RestAssuredJsonProperty.toBe("status", state)
            )
        )
    }

    fun offerJwtCredential(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val kid = cloudAgent.recall<String>("kid")
        val schemaUrl = cloudAgent.recall<String>("schema_url")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val credential = CreateIssueCredentialRecordRequest(
            claims = mapOf("automation-required" to "required value"),
            issuingDID = did,
            issuingKid = kid,
            connectionId = UUID.fromString(connectionId),
            schemaId = schemaUrl,
            automaticIssuance = true
        )

        cloudAgent.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credential),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("recordId", lastResponse().get<String>("recordId"))
    }

    fun offerSDJWTCredential(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val kid = cloudAgent.recall<String>("kid")
        val schemaUrl = cloudAgent.recall<String>("schema_url")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val credential = CreateIssueCredentialRecordRequest(
            claims = mapOf("automation-required" to "required value"),
            issuingDID = did,
            issuingKid = kid,
            connectionId = UUID.fromString(connectionId),
            schemaId = schemaUrl,
            automaticIssuance = true,
            validityPeriod = 36000.0,
            credentialFormat = "SDJWT"
        )

        cloudAgent.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credential),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("recordId", lastResponse().get<String>("recordId"))
    }

    fun offerAnonymousCredential(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val kid = cloudAgent.recall<String>("kid")
        val definitionGuid = cloudAgent.recall<String>("definition_guid")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val credential = CreateIssueCredentialRecordRequest(
            claims = mapOf(
                "name" to "automation",
                "age" to "99",
                "gender" to "M"
            ),
            automaticIssuance = true,
            issuingDID = did,
            issuingKid = kid,
            connectionId = UUID.fromString(connectionId),
            credentialFormat = "AnonCreds",
            credentialDefinitionId = UUID.fromString(definitionGuid)
        )

        cloudAgent.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credential),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("recordId", lastResponse().get<String>("recordId"))
    }

    fun createJwtConnectionlessCredentialOfferInvitation(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val kid = cloudAgent.recall<String>("kid")
        val schemaUrl = cloudAgent.recall<String>("schema_url")

        val offer = CreateIssueCredentialRecordRequest(
            validityPeriod = 3600.0,
            credentialFormat = "JWT",
            claims = mapOf("automation-required" to UUID.randomUUID().toString()),
            automaticIssuance = true,
            issuingDID = did,
            issuingKid = kid,
            schemaId = schemaUrl,
            goalCode = "automation-connectionless-jwt-issuance",
            goal = "automation"
        )

        cloudAgent.attemptsTo(
            Post.to("/issue-credentials/credential-offers/invitation").body(offer),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("invitation", lastResponse().get<String>("invitation.invitationUrl"))
        cloudAgent.remember("recordId", lastResponse().get<String>("recordId"))
    }

    fun createJwtConnectionlessVerificationInvite(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val schemaUrl = cloudAgent.recall<String>("schema_url")

        val proofs = ProofRequestAux(
            schemaId = schemaUrl,
            trustIssuers = listOf(did)
        )
        val presentProofRequest = RequestPresentationInput(
            options = Options(challenge = UUID.randomUUID().toString(), domain = Environment.agent.url),
            goalCode = "automation-connectionless-jwt-verification",
            goal = "automation",
            credentialFormat = "JWT",
            proofs = listOf(proofs)
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations/invitation").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("invitation", lastResponse().get<String>("invitation.invitationUrl"))
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun askForPresentProof(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val schemaUrl = cloudAgent.recall<String>("schema_url")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val presentProofRequest = RequestPresentationInput(
            connectionId = UUID.fromString(connectionId),
            options = Options(challenge = UUID.randomUUID().toString(), domain = Environment.agent.url),
            proofs = listOf(ProofRequestAux(schemaId = schemaUrl, trustIssuers = listOf(did))),
            credentialFormat = "JWT"
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun askForSDJWTPresentProof(cloudAgent: Actor) {
        val did = cloudAgent.recall<String>("did")
        val schemaUrl = cloudAgent.recall<String>("schema_url")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val presentProofRequest = RequestPresentationInput(
            connectionId = UUID.fromString(connectionId),
            options = Options(challenge = UUID.randomUUID().toString(), domain = Environment.agent.url),
            proofs = listOf(ProofRequestAux(schemaId = schemaUrl, trustIssuers = listOf(did))),
            credentialFormat = "SDJWT",
            claims = emptyMap<String, String>()
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun askForPresentProofAnonCreds(cloudAgent: Actor) {
        val definitionId = cloudAgent.recall<String>("definition_id")
        val connectionId = cloudAgent.recall<String>("connectionId")

        val anoncredRequest = AnoncredPresentationRequestV1(
            name = "proof_req_1",
            nonce = Utils.generateNonce(25),
            version = "0.1",
            requestedAttributes = mapOf(
                "gender" to AnoncredRequestedAttributeV1(
                    name = "gender",
                    restrictions = listOf(mapOf("attr::gender::value" to "M", "cred_def_id" to definitionId))
                )
            ),
            requestedPredicates = mapOf(
                "age" to AnoncredRequestedPredicateV1(
                    name = "age",
                    pType = ">=",
                    pValue = 18,
                    restrictions = listOf(mapOf("cred_def_id" to definitionId))
                )
            )
        )

        val presentProofRequest = RequestPresentationInput(
            connectionId = UUID.fromString(connectionId),
            credentialFormat = "AnonCreds",
            anoncredPresentationRequest = anoncredRequest,
            proofs = emptyList(),
            options = null
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun askForPresentProofAnonCredsWithUnexpectedAttributes(cloudAgent: Actor) {
        val definitionUrl = Environment.secp256k1.credDefUrl.id // Adapted from TS logic
        val connectionId = cloudAgent.recall<String>("connectionId")

        val anoncredRequest = AnoncredPresentationRequestV1(
            name = "proof_req_1",
            nonce = Utils.generateNonce(25),
            version = "0.1",
            requestedAttributes = mapOf(
                "driversLicense" to AnoncredRequestedAttributeV1(
                    name = "driversLicense",
                    restrictions = listOf(mapOf("attr::driversLicense::value" to "B", "cred_def_id" to definitionUrl))
                )
            ),
            requestedPredicates = emptyMap()
        )

        val presentProofRequest = RequestPresentationInput(
            connectionId = UUID.fromString(connectionId),
            credentialFormat = "AnonCreds",
            anoncredPresentationRequest = anoncredRequest,
            proofs = emptyList(),
            options = null
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun askForPresentProofAnonCredsWithUnexpectedValues(cloudAgent: Actor) {
        val definitionUrl = Environment.secp256k1.credDefUrl.id
        val connectionId = cloudAgent.recall<String>("connectionId")

        val anoncredRequest = AnoncredPresentationRequestV1(
            name = "proof_req_1",
            nonce = Utils.generateNonce(25),
            version = "0.1",
            requestedAttributes = mapOf(
                "name" to AnoncredRequestedAttributeV1(
                    name = "name",
                    restrictions = listOf(mapOf("attr::name::value" to "John", "cred_def_id" to definitionUrl))
                )
            ),
            requestedPredicates = emptyMap()
        )

        val presentProofRequest = RequestPresentationInput(
            connectionId = UUID.fromString(connectionId),
            credentialFormat = "AnonCreds",
            anoncredPresentationRequest = anoncredRequest,
            proofs = emptyList(),
            options = null
        )

        cloudAgent.attemptsTo(
            Post.to("/present-proof/presentations").body(presentProofRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
        cloudAgent.remember("presentationId", lastResponse().get<String>("presentationId"))
    }

    fun revokeCredential(cloudAgent: Actor, numberOfRevokedCredentials: Int) {
        val revokedRecordIdList = mutableListOf<String>()
        val recordIdList = cloudAgent.recall<MutableList<String>>("recordIdList")

        repeat(numberOfRevokedCredentials) {
            val recordId = recordIdList.removeAt(0)

            val statusListUrl = getStatusListCredentialUrl(cloudAgent, recordId)
            val encodedListBefore = getStatusListEncodedList(statusListUrl)

            cloudAgent.attemptsTo(
                Patch.to("/credential-status/revoke-credential/$recordId"),
                Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
            )

            Wait.until(60.seconds, 1.seconds) {
                getStatusListEncodedList(statusListUrl) != encodedListBefore
            }

            revokedRecordIdList.add(recordId)
        }

        cloudAgent.remember("recordIdList", recordIdList)
        cloudAgent.remember("revokedRecordIdList", revokedRecordIdList)
    }

    private fun getStatusListCredentialUrl(cloudAgent: Actor, recordId: String): String {
        cloudAgent.attemptsTo(
            Get.resource("/issue-credentials/records/$recordId"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
        val jwt = lastResponse().get<String>("credential")
        return extractStatusListUrlFromJwt(jwt)
    }

    private fun extractStatusListUrlFromJwt(jwt: String): String {
        val rawJwt = if (jwt.contains('.')) {
            jwt
        } else {
            String(Base64.getUrlDecoder().decode(jwt), StandardCharsets.UTF_8)
        }
        val parts = rawJwt.split(".")
        require(parts.size >= 2) { "Invalid JWT format, expected header.payload.signature" }
        val decodedPayload = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
        val payloadJson = JsonParser.parseString(decodedPayload).asJsonObject
        val vc = payloadJson.getAsJsonObject("vc")
        val credentialStatus = vc.getAsJsonObject("credentialStatus")
        return credentialStatus.get("statusListCredential").asString
    }

    private fun getStatusListEncodedList(statusListUrl: String): String {
        val response = SerenityRest.given()
            .get(statusListUrl)
            .thenReturn()
        assertThat(response.statusCode).isEqualTo(HttpStatus.SC_OK)
        return response.body.jsonPath().getString("credentialSubject.encodedList")
    }
}
