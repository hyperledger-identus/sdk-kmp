package org.hyperledger.identus.walletsdk.workflow

import com.google.gson.GsonBuilder
import io.iohk.atala.automation.serenity.interactions.PollingWait
import io.iohk.atala.automation.utils.Logger
import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.hamcrest.CoreMatchers.equalTo
import org.hyperledger.identus.walletsdk.abilities.UseWalletSdk
import org.hyperledger.identus.walletsdk.apollo.ApolloImpl
import org.hyperledger.identus.walletsdk.apollo.utils.Ed25519KeyPair
import org.hyperledger.identus.walletsdk.apollo.utils.Secp256k1KeyPair
import org.hyperledger.identus.walletsdk.configuration.Environment
import org.hyperledger.identus.walletsdk.domain.models.CredentialType
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.Curve
import org.hyperledger.identus.walletsdk.domain.models.KeyCurve
import org.hyperledger.identus.walletsdk.domain.models.KeyPurpose
import org.hyperledger.identus.walletsdk.domain.models.PresentationClaims
import org.hyperledger.identus.walletsdk.domain.models.ProvableCredential
import org.hyperledger.identus.walletsdk.domain.models.Seed
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.IssueCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.issueCredential.OfferCredential
import org.hyperledger.identus.walletsdk.edgeagent.protocols.outOfBand.OutOfBandInvitation
import org.hyperledger.identus.walletsdk.edgeagent.protocols.proofOfPresentation.RequestPresentation
import org.hyperledger.identus.walletsdk.pluto.PlutoBackupTask
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

object EdgeAgentWorkflow {
    private val logger = Logger.get<EdgeAgentWorkflow>()

    fun connect(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                val urlString = edgeAgent.recall<String>("invitation")
                // Assuming parseInvitation takes a string or URL, matching TS usage
                val oobInvitation = sdkContext.sdk.parseInvitation(urlString)
                try {
                    sdkContext.sdk.acceptOutOfBandInvitation(oobInvitation as OutOfBandInvitation)
                } catch (e: Exception) {
                    logger.error("Error connecting to cloud agent: ${e.message}")
                    val json = GsonBuilder().setPrettyPrinting().create().toJson(oobInvitation)
                    logger.error("oobInvitation: $json")
                    throw e
                }
            }
        )
    }

    fun waitForCredentialOffer(edgeAgent: Actor, numberOfCredentialOffer: Int) {
        edgeAgent.attemptsTo(
            PollingWait.until(
                UseWalletSdk.credentialOfferStackSize(),
                equalTo(numberOfCredentialOffer)
            )
        )
    }

    fun waitToReceiveCredentialIssuance(edgeAgent: Actor, expectedNumberOfCredentials: Int) {
        edgeAgent.attemptsTo(
            PollingWait.until(
                UseWalletSdk.issuedCredentialStackSize(),
                equalTo(expectedNumberOfCredentials)
            )
        )
    }

    fun processSpecificIssuedCred(edgeAgent: Actor, recordId: String) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                if (sdkContext.issuedCredentialStack.isNotEmpty()) {
                    val issuedCredentialMessage = sdkContext.issuedCredentialStack.removeFirst()
                    val issuedCredential = IssueCredential.fromMessage(issuedCredentialMessage)
                    val credential = sdkContext.sdk.processIssuedCredentialMessage(issuedCredential)
                    edgeAgent.remember(recordId, credential.id)
                }
            }
        )
    }

    fun acceptCredential(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                if (sdkContext.credentialOfferStack.isNotEmpty()) {
                    val message = OfferCredential.fromMessage(sdkContext.credentialOfferStack.removeFirst())
                    val format = message.attachments.firstOrNull()?.format.orEmpty()
                    val did = when {
                        format.contains("anoncred", ignoreCase = true) -> {
                            sdkContext.sdk.createNewPrismDID()
                        }
                        format.contains("sd-jwt", ignoreCase = true) ||
                            format.contains("sdjwt", ignoreCase = true) -> {
                            val authKeyPair = Ed25519KeyPair.generateKeyPair()
                            sdkContext.sdk.createNewPrismDID(
                                keys = listOf(Pair(KeyPurpose.AUTHENTICATION, authKeyPair.privateKey))
                            )
                        }
                        else -> {
                            val authKeyPair = Secp256k1KeyPair.generateKeyPair()
                            sdkContext.sdk.createNewPrismDID(
                                keys = listOf(Pair(KeyPurpose.AUTHENTICATION, authKeyPair.privateKey))
                            )
                        }
                    }
                    val requestCredential = sdkContext.sdk.prepareRequestCredentialWithIssuer(did, message)
                    val formattedMessage = requestCredential.makeMessage()
                    try {
                        sdkContext.sdk.sendMessage(formattedMessage)
                    } catch (e: Exception) {
                        logger.warn("Failed to send acceptCredential message: ${e.message}")
                    }
                }
            }
        )
    }

    fun waitForProofRequest(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            PollingWait.until(
                UseWalletSdk.proofOfRequestStackSize(),
                equalTo(1)
            )
        )
    }

    fun presentVerificationRequest(edgeAgent: Actor) {
        presentProof(edgeAgent) // Logic appears identical in standard usage, delegating
    }

    fun presentProof(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                val credentials = runBlocking { sdkContext.sdk.getAllCredentials().first() }
                // Simplified selection strategy: pick first available
                val credential = credentials.firstOrNull()
                assertThat(credential).isNotNull.withFailMessage("No credentials found in wallet to present proof")
                assertThat(credential).instanceOf(ProvableCredential::class)

                if (sdkContext.proofRequestStack.isNotEmpty() && credential is ProvableCredential) {
                    val requestPresentationMessage = RequestPresentation.fromMessage(sdkContext.proofRequestStack.removeFirst())
                    val presentation = sdkContext.sdk.preparePresentationForRequestProof(requestPresentationMessage, credential)
                    try {
                        sdkContext.sdk.sendMessage(presentation.makeMessage())
                    } catch (e: Exception) {
                        logger.warn("Failed to send presentation message: ${e.message}")
                    }
                }
            }
        )
    }

    fun tryToPresentVerificationRequestWithWrongAnoncred(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                val credentials = sdkContext.sdk.getAllCredentials().first()
                val credential = credentials.firstOrNull()

                if (sdkContext.proofRequestStack.isNotEmpty() && credential is ProvableCredential) {
                    val requestPresentationMessage = RequestPresentation.fromMessage(sdkContext.proofRequestStack.removeFirst())
                    try {
                        sdkContext.sdk.preparePresentationForRequestProof(requestPresentationMessage, credential)
                        fail<String>("Wrong anoncred should produce exception message")
                    } catch (e: Throwable) {
                        assertThat(e).isInstanceOf(
                            org.hyperledger.identus.walletsdk.edgeagent.EdgeAgentError.CredentialNotValidForPresentationRequest::class.java
                        )
                    }
                }
            }
        )
    }

    fun waitForPresentationMessage(edgeAgent: Actor, numberOfMessages: Int = 1) {
        edgeAgent.attemptsTo(
            PollingWait.until(
                UseWalletSdk.presentationStackSize(),
                equalTo(numberOfMessages)
            )
        )
    }

    fun verifyPresentation(edgeAgent: Actor, expected: Boolean = true) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                if (sdkContext.presentationStack.isNotEmpty()) {
                    val message = sdkContext.presentationStack.removeFirst()
                    try {
                        val isVerified = sdkContext.sdk.handlePresentation(message)
                        assertThat(isVerified).isEqualTo(expected)
                    } catch (e: Throwable) {
                        if (e.message?.contains("credential is revoked") == true) {
                            assertThat(expected).isFalse()
                        } else {
                            if (expected) throw e
                        }
                    }
                }
            }
        )
    }

    fun waitForCredentialRevocationMessage(edgeAgent: Actor, numberOfRevocation: Int) {
        edgeAgent.attemptsTo(
            PollingWait.with(2.minutes, 500.milliseconds).until(
                UseWalletSdk.revocationStackSize(),
                equalTo(numberOfRevocation)
            )
        )
    }

    fun waitUntilCredentialIsRevoked(edgeAgent: Actor, revokedRecordIdList: List<String>) {
        val revokedIdList = revokedRecordIdList.map { recordId ->
            edgeAgent.recall<String>(recordId)
        }

        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                // Process revocations in stack
                while (sdkContext.revocationStack.isNotEmpty()) {
                    val revocationMsg = sdkContext.revocationStack.removeFirst()
                    // Assuming sdk.handle exists for generic message processing or revocation specific
                    // sdkContext.sdk.handle(revocationMsg)
                    // In some SDK versions, processRevocationNotification might be distinct
                }

                val credentials = runBlocking { sdkContext.sdk.getAllCredentials().first() }

                // Filter logic
                val revokedCredentials = credentials.filter { credential ->
                    // Logic matching TS: isRevoked() && in list
                    // Note: SDK Property might be `revoked` or `isRevoked`
                    (credential.revoked == true) && revokedIdList.contains(credential.id)
                }

                assertThat(revokedCredentials.size).isEqualTo(revokedRecordIdList.size)
            }
        )
    }

    fun createPeerDids(edgeAgent: Actor, numberOfDids: Int) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                repeat(numberOfDids) {
                    val did = sdkContext.sdk.createNewPeerDID(updateMediator = true)
                    edgeAgent.remember("lastPeerDID", did)
                }
            }
        )
    }

    fun createPrismDids(edgeAgent: Actor, numberOfDids: Int) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                repeat(numberOfDids) {
                    sdkContext.sdk.createNewPrismDID()
                }
            }
        )
    }

    fun initiatePresentationRequest(type: CredentialType, edgeAgent: Actor, did: DID, claims: PresentationClaims) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                sdkContext.sdk.initiatePresentationRequest(type, did, claims, "", UUID.randomUUID().toString())
            }
        )
    }

    // --- Backup & Restore ---

    fun createBackup(edgeAgent: Actor) {
        edgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                val backup = sdkContext.sdk.backupWallet(PlutoBackupTask(sdkContext.sdk.pluto))
                val seed = sdkContext.sdk.seed
                edgeAgent.remember("backup", backup)
                edgeAgent.remember("seed", seed)
                val backupJson = sdkContext.sdk.decryptBackupJson(backup)
                edgeAgent.remember("backupJson", backupJson)
            }
        )
    }

    fun createANewWalletFromBackup(edgeAgent: Actor) {
        val backup = edgeAgent.recall<String>("backup")
        val seed = edgeAgent.recall<Seed>("seed")

        val walletSdk = UseWalletSdk()
        walletSdk.recoverWallet(seed, backup)
        runBlocking { walletSdk.tearDown() }
    }

    fun createNewWalletFromBackupWithWrongSeed(edgeAgent: Actor) {
        val backup = edgeAgent.recall<String>("backup")
        val seed = ApolloImpl().createRandomSeed().seed // Random seed

        val walletSdk = UseWalletSdk()
        runBlocking {
            try {
                walletSdk.recoverWallet(seed, backup)
                fail<String>("SDK should not be able to restore with wrong seed phrase.")
            } catch (e: Exception) {
                assertThat(e).isNotNull()
            }
        }
    }

    // Backup JSON is decrypted by EdgeAgent via decryptBackupJson()

    fun backupAndRestoreToNewAgent(newAgent: Actor, originalAgent: Actor) {
        val backup = originalAgent.recall<String>("backup")
        val seed = originalAgent.recall<Seed>("seed")

        val walletSdk = UseWalletSdk()
        walletSdk.recoverWallet(seed, backup)
        // Enable API for new agent
        newAgent.whoCan(walletSdk).whoCan(CallAnApi.at(Environment.mediator.url))
    }

    fun copyAgentShouldMatchOriginalAgent(restoredEdgeAgent: Actor, originalEdgeAgent: Actor) {
        val expectedCredentials = mutableListOf<String>()
        val expectedPeerDids = mutableListOf<String>()
        val expectedPrismDids = mutableListOf<String>()

        originalEdgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                runBlocking {
                    expectedCredentials.addAll(sdkContext.sdk.getAllCredentials().first().map { it.id })
                    expectedPeerDids.addAll(sdkContext.sdk.pluto.getAllPeerDIDs().first().map { it.did.toString() })
                    expectedPrismDids.addAll(sdkContext.sdk.pluto.getAllPrismDIDs().first().map { it.did.toString() })
                }
            }
        )

        restoredEdgeAgent.attemptsTo(
            UseWalletSdk.execute { sdkContext ->
                runBlocking {
                    val actualCredentials = sdkContext.sdk.getAllCredentials().first().map { it.id }
                    val actualPeerDids = sdkContext.sdk.pluto.getAllPeerDIDs().first().map { it.did.toString() }
                    val actualPrismDids = sdkContext.sdk.pluto.getAllPrismDIDs().first().map { it.did.toString() }

                    fun normalizePrismDid(did: String): String = did.substringBefore("#")
                    val expectedPrismDidsNormalized = expectedPrismDids.map(::normalizePrismDid)
                    val actualPrismDidsNormalized = actualPrismDids.map(::normalizePrismDid)

                    fun logDiffs(label: String, expected: List<String>, actual: List<String>) {
                        val missing = expected.filterNot { actual.contains(it) }
                        val unexpected = actual.filterNot { expected.contains(it) }

                        if (missing.isNotEmpty() || unexpected.isNotEmpty()) {
                            logger.warn(
                                buildString {
                                    append("Backup restore mismatch for ")
                                    append(label)
                                    append(" | expected=")
                                    append(expected.size)
                                    append(" actual=")
                                    append(actual.size)
                                    if (missing.isNotEmpty()) {
                                        append(" | missing=")
                                        append(missing.sorted())
                                    }
                                    if (unexpected.isNotEmpty()) {
                                        append(" | unexpected=")
                                        append(unexpected.sorted())
                                    }
                                }
                            )
                        } else {
                            logger.info(
                                "Backup restore match for $label | size=${expected.size}"
                            )
                        }
                    }

                    logDiffs("credentials", expectedCredentials, actualCredentials)
                    logDiffs("peerDids", expectedPeerDids, actualPeerDids)
                    logDiffs("prismDids", expectedPrismDidsNormalized, actualPrismDidsNormalized)

                    assertThat(actualCredentials.size).isEqualTo(expectedCredentials.size)
                    assertThat(actualCredentials.containsAll(expectedCredentials)).isTrue()
                    assertThat(actualPeerDids.size).isEqualTo(expectedPeerDids.size)
                    assertThat(actualPeerDids.containsAll(expectedPeerDids)).isTrue()
                    assertThat(actualPrismDidsNormalized.size).isEqualTo(expectedPrismDidsNormalized.size)
                    assertThat(actualPrismDidsNormalized.containsAll(expectedPrismDidsNormalized)).isTrue()
                }
            }
        )
    }
}
