package org.hyperledger.identus.walletsdk.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import org.hyperledger.identus.walletsdk.domain.models.AnoncredsPresentationClaims
import org.hyperledger.identus.walletsdk.domain.models.CredentialType
import org.hyperledger.identus.walletsdk.domain.models.DID
import org.hyperledger.identus.walletsdk.domain.models.InputFieldFilter
import org.hyperledger.identus.walletsdk.domain.models.JWTPresentationClaims
import org.hyperledger.identus.walletsdk.domain.models.RequestedAttributes
import org.hyperledger.identus.walletsdk.workflow.CloudAgentWorkflow
import org.hyperledger.identus.walletsdk.workflow.EdgeAgentWorkflow

class EdgeAgentSteps {

    @Given("{actor} has '{int}' jwt credentials issued by {actor}")
    fun edgeAgentHasJwtCredentialsIssuedByCloudAgent(edgeAgent: Actor, numberOfIssuedCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfIssuedCredentials) {
            CloudAgentWorkflow.offerJwtCredential(cloudAgent)
            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            recordIdList.add(recordId)
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
            EdgeAgentWorkflow.waitToReceiveCredentialIssuance(edgeAgent, 1)
            EdgeAgentWorkflow.processSpecificIssuedCred(edgeAgent, recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @Given("{actor} has '{int}' sd+jwt credentials issued by {actor}")
    fun edgeAgentHasSdJwtCredentialsIssuedByCloudAgent(edgeAgent: Actor, numberOfIssuedCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfIssuedCredentials) {
            CloudAgentWorkflow.offerSDJWTCredential(cloudAgent)
            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            recordIdList.add(recordId)
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
            EdgeAgentWorkflow.waitToReceiveCredentialIssuance(edgeAgent, 1)
            EdgeAgentWorkflow.processSpecificIssuedCred(edgeAgent, recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @Given("{actor} has '{int}' anonymous credentials issued by {actor}")
    fun edgeAgentHasAnonymousCredentialsIssuedByCloudAgent(edgeAgent: Actor, numberOfIssuedCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfIssuedCredentials) {
            CloudAgentWorkflow.offerAnonymousCredential(cloudAgent)
            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            recordIdList.add(recordId)
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
            EdgeAgentWorkflow.waitToReceiveCredentialIssuance(edgeAgent, 1)
            EdgeAgentWorkflow.processSpecificIssuedCred(edgeAgent, recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @Given("{actor} has '{int}' connectionless jwt credentials issued by {actor}")
    fun edgeAgentHasConnectionlessJwtCredentialsIssuedByCloudAgent(edgeAgent: Actor, numberOfIssuedCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfIssuedCredentials) {
            CloudAgentWorkflow.createJwtConnectionlessCredentialOfferInvitation(cloudAgent)
            CloudAgentWorkflow.shareInvitation(cloudAgent, edgeAgent)
            EdgeAgentWorkflow.connect(edgeAgent)

            recordIdList.add("connectionless")

            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            EdgeAgentWorkflow.waitToReceiveCredentialIssuance(edgeAgent, 1)
            EdgeAgentWorkflow.processSpecificIssuedCred(edgeAgent, "connectionless")
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @Given("{actor} has created a backup")
    fun edgeAgentHasCreatedBackup(edgeAgent: Actor) {
        EdgeAgentWorkflow.createBackup(edgeAgent)
    }

    @Given("{actor} creates '{}' peer DIDs")
    fun edgeAgentCreatesPeerDids(edgeAgent: Actor, numberOfDids: Int) {
        EdgeAgentWorkflow.createPeerDids(edgeAgent, numberOfDids)
    }

    @Given("{actor} creates '{}' prism DIDs")
    fun edgeAgentCreatesPrismDids(edgeAgent: Actor, numberOfDids: Int) {
        EdgeAgentWorkflow.createPrismDids(edgeAgent, numberOfDids)
    }

    @When("{actor} accepts {int} sd+jwt credential offer sequentially from {actor}")
    fun edgeAgentAcceptsSdJwtCredentialOffersSequentially(edgeAgent: Actor, numberOfCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerSDJWTCredential(cloudAgent)
            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
            recordIdList.add(recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @When("{actor} accepts {int} jwt credential offer sequentially from {actor}")
    fun edgeAgentAcceptsJwtCredentialOffersSequentially(edgeAgent: Actor, numberOfCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerJwtCredential(cloudAgent)
            EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, 1)
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
            recordIdList.add(recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @When("{actor} accepts {int} jwt credentials offer at once from {actor}")
    fun edgeAgentAcceptsJwtCredentialsOfferAtOnce(edgeAgent: Actor, numberOfCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerJwtCredential(cloudAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            recordIdList.add(recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)

        EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, numberOfCredentials)

        repeat(numberOfCredentials) {
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
        }
    }

    @When("{actor} accepts {int} sd+jwt credentials offer at once from {actor}")
    fun edgeAgentAcceptsSdJwtCredentialsOfferAtOnce(edgeAgent: Actor, numberOfCredentials: Int, cloudAgent: Actor) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerSDJWTCredential(cloudAgent)
            val recordId = cloudAgent.recall<String>("recordId")
            recordIdList.add(recordId)
        }
        cloudAgent.remember("recordIdList", recordIdList)

        EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, numberOfCredentials)

        repeat(numberOfCredentials) {
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
        }
    }

    @When("{actor} connects through the invite")
    fun edgeAgentConnectsThroughInvite(edgeAgent: Actor) {
        EdgeAgentWorkflow.connect(edgeAgent)
    }

    @When("{actor} accepts the credentials offer from {actor}")
    fun edgeAgentAcceptsCredentialsOfferFromCloudAgent(edgeAgent: Actor, cloudAgent: Actor) {
        val recordIdList = cloudAgent.recall<List<String>>("recordIdList")
        repeat(recordIdList.size) {
            EdgeAgentWorkflow.acceptCredential(edgeAgent)
        }
    }

    @When("{actor} sends the present-proof")
    fun edgeAgentSendsPresentProof(edgeAgent: Actor) {
        EdgeAgentWorkflow.waitForProofRequest(edgeAgent)
        EdgeAgentWorkflow.presentProof(edgeAgent)
    }

    @Then("{actor} should receive the credentials offer from {actor}")
    fun edgeAgentShouldReceiveCredentialsOffer(edgeAgent: Actor, cloudAgent: Actor) {
        val recordIdList = cloudAgent.recall<List<String>>("recordIdList")
        EdgeAgentWorkflow.waitForCredentialOffer(edgeAgent, recordIdList.size)
    }

    @Then("{actor} waits to receive the revocation notifications from {actor}")
    fun edgeAgentWaitsToReceiveRevocationNotifications(edgeAgent: Actor, cloudAgent: Actor) {
        val revokedRecordIdList = cloudAgent.recall<List<String>>("revokedRecordIdList")
        EdgeAgentWorkflow.waitForCredentialRevocationMessage(edgeAgent, revokedRecordIdList.size)
    }

    @Then("{actor} should see the credentials were revoked by {actor}")
    fun edgeAgentShouldSeeCredentialsWereRevoked(edgeAgent: Actor, cloudAgent: Actor) {
        val revokedRecordIdList = cloudAgent.recall<List<String>>("revokedRecordIdList")
        EdgeAgentWorkflow.waitUntilCredentialIsRevoked(edgeAgent, revokedRecordIdList)
    }

    @Then("{actor} process issued credentials from {actor}")
    fun edgeAgentProcessesIssuedCredentials(edgeAgent: Actor, cloudAgent: Actor) {
        val recordIdList = cloudAgent.recall<List<String>>("recordIdList")
        for (recordId in recordIdList) {
            EdgeAgentWorkflow.processSpecificIssuedCred(edgeAgent, recordId)
        }
    }

    @Then("{actor} wait to receive issued credentials from {actor}")
    fun edgeAgentWaitsToReceiveIssuedCredentials(edgeAgent: Actor, cloudAgent: Actor) {
        val recordIdList = cloudAgent.recall<List<String>>("recordIdList")
        EdgeAgentWorkflow.waitToReceiveCredentialIssuance(edgeAgent, recordIdList.size)
    }

    @Then("a new SDK can be restored from {actor}")
    fun newSdkCanBeRestoredFromEdgeAgent(edgeAgent: Actor) {
        EdgeAgentWorkflow.createANewWalletFromBackup(edgeAgent)
    }

    @Then("a new SDK cannot be restored from {actor} with wrong seed")
    fun newSdkCannotBeRestoredWithWrongSeed(edgeAgent: Actor) {
        EdgeAgentWorkflow.createNewWalletFromBackupWithWrongSeed(edgeAgent)
    }

    @Then("a new {actor} is restored from {actor}")
    fun newAgentIsRestoredFromEdgeAgent(newAgent: Actor, edgeAgent: Actor) {
        EdgeAgentWorkflow.backupAndRestoreToNewAgent(newAgent, edgeAgent)
    }

    @Then("{actor} should have the expected values from {actor}")
    fun restoredAgentShouldHaveExpectedValues(copyEdgeAgent: Actor, originalEdgeAgent: Actor) {
        EdgeAgentWorkflow.copyAgentShouldMatchOriginalAgent(copyEdgeAgent, originalEdgeAgent)
    }

    @Then("{actor} is dismissed")
    fun edgeAgentIsDismissed(edgeAgent: Actor) {
        edgeAgent.wrapUp()
    }

    @Then("{actor} will request {actor} to verify the anonymous credential")
    fun verifierRequestsHolderToVerifyAnonymousCredential(verifierEdgeAgent: Actor, holderEdgeAgent: Actor) {
        EdgeAgentWorkflow.createPeerDids(holderEdgeAgent, 1)
        val holderDID = holderEdgeAgent.recall<DID>("lastPeerDID")
        val claims = AnoncredsPresentationClaims(
            attributes = mapOf(
                "name" to RequestedAttributes(name = "name", names = setOf("name"), restrictions = emptyMap(), null)
            ),
            predicates = emptyMap()
        )
        EdgeAgentWorkflow.initiatePresentationRequest(CredentialType.ANONCREDS_PROOF_REQUEST, verifierEdgeAgent, holderDID, claims)
    }

    @Then("{actor} will request {actor} to verify the JWT credential")
    fun verifierRequestsHolderToVerifyJwtCredential(verifierEdgeAgent: Actor, holderEdgeAgent: Actor) {
        EdgeAgentWorkflow.createPeerDids(holderEdgeAgent, 1)
        val holderDID = holderEdgeAgent.recall<DID>("lastPeerDID")
        val claims = JWTPresentationClaims(
            claims = mapOf("automation-required" to InputFieldFilter(type = "string", pattern = "required value"))
        )
        EdgeAgentWorkflow.initiatePresentationRequest(CredentialType.JWT, verifierEdgeAgent, holderDID, claims)
    }

    @Then("{actor} will request {actor} to verify the SD+JWT credential")
    fun verifierRequestsHolderToVerifySdJwtCredential(verifierEdgeAgent: Actor, holderEdgeAgent: Actor) {
        EdgeAgentWorkflow.createPeerDids(holderEdgeAgent, 1)
        val holderDID = holderEdgeAgent.recall<DID>("lastPeerDID")
        val claims = JWTPresentationClaims(
            claims = mapOf("automation-required" to InputFieldFilter(type = "string", pattern = "required value"))
        )
        // Mapping CredentialType.SDJWT if available in SDK, otherwise assume JWT structure handled
        EdgeAgentWorkflow.initiatePresentationRequest(CredentialType.SDJWT, verifierEdgeAgent, holderDID, claims)
    }

    @Then("{actor} will request {actor} to verify the SD+JWT credential with non-existing claims")
    fun verifierRequestsHolderToVerifySdJwtCredentialWithNonExistingClaims(verifierEdgeAgent: Actor, holderEdgeAgent: Actor) {
        EdgeAgentWorkflow.createPeerDids(holderEdgeAgent, 1)
        val holderDID = holderEdgeAgent.recall<DID>("lastPeerDID")
        val claims = JWTPresentationClaims(
            claims = mapOf("doesNotExist" to InputFieldFilter(type = "string", pattern = "required value"))
        )
        EdgeAgentWorkflow.initiatePresentationRequest(CredentialType.SDJWT, verifierEdgeAgent, holderDID, claims)
    }

    @When("{actor} sends the verification proof")
    fun edgeAgentSendsVerificationProof(edgeAgent: Actor) {
        EdgeAgentWorkflow.waitForProofRequest(edgeAgent)
        EdgeAgentWorkflow.presentVerificationRequest(edgeAgent)
    }

    @Then("{actor} should receive an exception when trying to use a wrong anoncred credential")
    fun edgeAgentShouldReceiveExceptionForWrongAnoncred(edgeAgent: Actor) {
        EdgeAgentWorkflow.waitForProofRequest(edgeAgent)
        EdgeAgentWorkflow.tryToPresentVerificationRequestWithWrongAnoncred(edgeAgent)
    }

    @Then("{actor} should see the verification proof is verified")
    fun verifierEdgeAgentShouldSeeVerificationProofVerified(verifierEdgeAgent: Actor) {
        EdgeAgentWorkflow.waitForPresentationMessage(verifierEdgeAgent)
        EdgeAgentWorkflow.verifyPresentation(verifierEdgeAgent, expected = true)
    }

    @Then("{actor} should see the verification proof is verified false")
    fun verifierEdgeAgentShouldSeeVerificationProofVerifiedFalse(verifierEdgeAgent: Actor) {
        EdgeAgentWorkflow.waitForPresentationMessage(verifierEdgeAgent)
        EdgeAgentWorkflow.verifyPresentation(verifierEdgeAgent, expected = false)
    }
}
