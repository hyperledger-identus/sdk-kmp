package org.hyperledger.identus.walletsdk.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import org.hyperledger.identus.walletsdk.configuration.Environment
import org.hyperledger.identus.walletsdk.workflow.CloudAgentWorkflow
import org.hyperledger.identus.walletsdk.workflow.EdgeAgentWorkflow
import java.lang.IllegalArgumentException

class CloudAgentSteps {

    @Given("{actor} has a connection invitation with '{}', '{}' and '{}' parameters")
    fun cloudAgentHasConnectionInvitation(cloudAgent: Actor, label: String, goalCode: String, goal: String) {
        val mappedLabel = if (label == "null") null else label
        val mappedGoalCode = if (goalCode == "null") null else goalCode
        val mappedGoal = if (goal == "null") null else goal
        CloudAgentWorkflow.createConnection(cloudAgent, mappedLabel, mappedGoalCode, mappedGoal)
    }

    @Given("{actor} is connected to {actor}")
    fun cloudAgentIsConnectedToEdgeAgent(cloudAgent: Actor, edgeAgent: Actor) {
        CloudAgentWorkflow.createConnection(cloudAgent)
        CloudAgentWorkflow.shareInvitation(cloudAgent, edgeAgent)
        EdgeAgentWorkflow.connect(edgeAgent)
        CloudAgentWorkflow.waitForConnectionState(cloudAgent, "ConnectionResponseSent")
    }

    @Given("{actor} is not connected to {actor}")
    fun cloudAgentIsNotConnectedToEdgeAgent(cloudAgent: Actor, edgeAgent: Actor) {
        CloudAgentWorkflow.hasNoConnection(cloudAgent, edgeAgent)
    }

    @Given("{actor} shares invitation to {actor}")
    fun cloudAgentSharesInvitationToEdgeAgent(cloudAgent: Actor, edgeAgent: Actor) {
        CloudAgentWorkflow.shareInvitation(cloudAgent, edgeAgent)
    }

    @Given("{actor} has a connectionless jwt credential offer invitation")
    fun cloudAgentHasConnectionlessJwtCredentialOfferInvitation(cloudAgent: Actor) {
        CloudAgentWorkflow.createJwtConnectionlessCredentialOfferInvitation(cloudAgent)
        cloudAgent.remember("recordIdList", mutableListOf("connectionless"))
    }

    @Given("{actor} has a connectionless jwt verification invite")
    fun cloudAgentHasConnectionlessJwtVerificationInvite(cloudAgent: Actor) {
        CloudAgentWorkflow.createJwtConnectionlessVerificationInvite(cloudAgent)
        cloudAgent.remember("recordIdList", mutableListOf("connectionless"))
    }

    @Given("{actor} uses did='{}' and kid='{}' for issuance")
    fun cloudAgentUsesDidAndKidForIssuance(cloudAgent: Actor, didKey: String, kid: String) {
        val didData = when (didKey) {
            "secp256k1" -> Environment.secp256k1
            "ed25519" -> Environment.ed25519
            else -> throw IllegalArgumentException("Configuration for did='$didKey' not found in Environment.")
        }
        cloudAgent.remember("kid", kid)
        cloudAgent.remember("did", didData.did)
    }

    @Given("{actor} uses jwt schema issued with did='{}'")
    fun cloudAgentUsesJwtSchemaIssuedWithDid(cloudAgent: Actor, didKey: String) {
        val didData = when (didKey) {
            "secp256k1" -> Environment.secp256k1
            "ed25519" -> Environment.ed25519
            else -> throw IllegalArgumentException("Configuration for did='$didKey' not found in Environment.")
        }
        cloudAgent.remember("schema_guid", didData.jwtSchema.guid)
        cloudAgent.remember("schema_url", didData.jwtSchema.url)
    }

    @Given("{actor} uses definition='{}' issued with did='{}'")
    fun cloudAgentUsesDefinitionIssuedWithDid(cloudAgent: Actor, def: String, didKey: String) {
        val didData = when (didKey) {
            "secp256k1" -> Environment.secp256k1
            "ed25519" -> Environment.ed25519
            else -> throw IllegalArgumentException("Configuration for did='$didKey' not found in Environment.")
        }
        if (def != "credDefUrl") {
            throw IllegalArgumentException("Configuration for definition='$def' not found in Environment.")
        }
        cloudAgent.remember("definition_guid", didData.credDefUrl.guid)
        cloudAgent.remember("definition_id", didData.credDefUrl.id)
    }

    @When("{actor} offers '{int}' jwt credentials")
    fun cloudAgentOffersJwtCredentials(cloudAgent: Actor, numberOfCredentials: Int) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerJwtCredential(cloudAgent)
            recordIdList.add(cloudAgent.recall("recordId"))
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @When("{actor} offers '{int}' sd+jwt credentials")
    fun cloudAgentOffersSdJwtCredentials(cloudAgent: Actor, numberOfCredentials: Int) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerSDJWTCredential(cloudAgent)
            recordIdList.add(cloudAgent.recall("recordId"))
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @When("{actor} offers '{int}' anonymous credential")
    fun cloudAgentOffersAnonymousCredential(cloudAgent: Actor, numberOfCredentials: Int) {
        val recordIdList = mutableListOf<String>()
        repeat(numberOfCredentials) {
            CloudAgentWorkflow.offerAnonymousCredential(cloudAgent)
            recordIdList.add(cloudAgent.recall("recordId"))
        }
        cloudAgent.remember("recordIdList", recordIdList)
    }

    @When("{actor} asks for sdjwt present-proof")
    fun cloudAgentAsksForSdjwtPresentProof(cloudAgent: Actor) {
        CloudAgentWorkflow.askForSDJWTPresentProof(cloudAgent)
    }

    @When("{actor} asks for present-proof")
    fun cloudAgentAsksForPresentProof(cloudAgent: Actor) {
        CloudAgentWorkflow.askForPresentProof(cloudAgent)
    }

    @When("{actor} asks for presentation of AnonCred proof")
    fun cloudAgentAsksForAnonCredProof(cloudAgent: Actor) {
        CloudAgentWorkflow.askForPresentProofAnonCreds(cloudAgent)
    }

    @When("{actor} asks for presentation of AnonCred proof with unexpected attributes")
    fun cloudAgentAsksForAnonCredProofWithUnexpectedAttributes(cloudAgent: Actor) {
        CloudAgentWorkflow.askForPresentProofAnonCredsWithUnexpectedAttributes(cloudAgent)
    }

    @When("{actor} asks for presentation of AnonCred proof with unexpected values")
    fun cloudAgentAsksForAnonCredProofWithUnexpectedValues(cloudAgent: Actor) {
        CloudAgentWorkflow.askForPresentProofAnonCredsWithUnexpectedValues(cloudAgent)
    }

    @When("{actor} revokes '{int}' credentials")
    fun cloudAgentRevokesCredentials(cloudAgent: Actor, numberOfRevokedCredentials: Int) {
        CloudAgentWorkflow.revokeCredential(cloudAgent, numberOfRevokedCredentials)
    }

    @Then("{actor} should have the connection status updated to '{}'")
    fun cloudAgentShouldHaveConnectionStatusUpdated(cloudAgent: Actor, expectedState: String) {
        CloudAgentWorkflow.waitForConnectionState(cloudAgent, expectedState)
    }

    @Then("{actor} should see the present-proof is verified")
    fun cloudAgentShouldSeePresentProofVerified(cloudAgent: Actor) {
        CloudAgentWorkflow.verifyPresentProof(cloudAgent, "PresentationVerified")
    }

    @Then("{actor} should see the present-proof is not verified")
    fun cloudAgentShouldSeePresentProofNotVerified(cloudAgent: Actor) {
        CloudAgentWorkflow.verifyPresentProof(cloudAgent, "PresentationFailed")
    }

    @Then("{actor} should see all credentials were accepted")
    fun cloudAgentShouldSeeAllCredentialsAccepted(cloudAgent: Actor) {
        val recordIdList = cloudAgent.recall<List<String>>("recordIdList")
        for (recordId in recordIdList) {
            CloudAgentWorkflow.verifyCredentialState(cloudAgent, recordId, "CredentialSent")
        }
    }
}
