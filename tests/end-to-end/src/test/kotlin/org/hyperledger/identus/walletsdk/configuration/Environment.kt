package org.hyperledger.identus.walletsdk.configuration

import io.iohk.atala.automation.utils.Wait
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.response.Response
import net.serenitybdd.rest.SerenityRest
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.hyperledger.identus.client.models.CreateManagedDidRequest
import org.hyperledger.identus.client.models.CreateManagedDidRequestDocumentTemplate
import org.hyperledger.identus.client.models.CredentialDefinitionInput
import org.hyperledger.identus.client.models.CredentialSchemaInput
import org.hyperledger.identus.client.models.Curve
import org.hyperledger.identus.client.models.ManagedDIDKeyTemplate
import org.hyperledger.identus.client.models.Purpose
import org.hyperledger.identus.walletsdk.models.AnoncredSchema
import org.hyperledger.identus.walletsdk.models.JwtSchema
import org.hyperledger.identus.walletsdk.models.JwtSchemaProperty
import org.hyperledger.identus.walletsdk.utils.Notes
import java.util.Properties
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class AgentConfig(
    var url: String,
    val apiKey: String
)

data class MediatorConfig(
    val url: String
)

data class JwtSchemaConfig(
    val guid: String,
    var url: String = ""
)

data class CredDefConfig(
    val guid: String,
    var id: String = ""
)

data class DataByDid(
    var did: String,
    var jwtSchema: JwtSchemaConfig,
    var credDefUrl: CredDefConfig
)

object Environment {
    lateinit var agent: AgentConfig
    lateinit var mediator: MediatorConfig
    lateinit var secp256k1: DataByDid
    lateinit var ed25519: DataByDid

    /**
     * Set up the variables based on the properties config file and Environment variables
     */
    fun setup() {
        Notes.prepareNotes()
        val properties = loadProperties()
        val agentUrlRaw = properties.getProperty("AGENT_URL") ?: ""
        agent = AgentConfig(
            url = if (agentUrlRaw.endsWith("/")) agentUrlRaw.dropLast(1) else agentUrlRaw,
            apiKey = properties.getProperty("APIKEY") ?: ""
        )
        mediator = MediatorConfig(
            url = properties.getProperty("MEDIATOR_OOB_URL") ?: ""
        )
        secp256k1 = DataByDid(
            did = properties.getProperty("SECP256K1_PUBLISHED_DID") ?: "",
            jwtSchema = JwtSchemaConfig(guid = properties.getProperty("SECP256K1_JWT_SCHEMA_GUID") ?: ""),
            credDefUrl = CredDefConfig(guid = properties.getProperty("SECP256K1_ANONCRED_DEFINITION_GUID") ?: "")
        )
        ed25519 = DataByDid(
            did = properties.getProperty("ED25519_PUBLISHED_DID") ?: "",
            jwtSchema = JwtSchemaConfig(guid = properties.getProperty("ED25519_JWT_SCHEMA_GUID") ?: ""),
            credDefUrl = CredDefConfig(guid = properties.getProperty("ED25519_ANONCRED_DEFINITION_GUID") ?: "")
        )
        configureRestAssured()
        secp256k1.did = verifyDidSetup(secp256k1.did, Curve.SECP256K1)
        secp256k1.jwtSchema = verifyJwtSchemaSetupUrl(secp256k1.jwtSchema.guid, secp256k1.did)
        secp256k1.credDefUrl = verifyAnoncredDefinitionUrl(secp256k1.credDefUrl.guid, secp256k1.did, secp256k1.did)

        ed25519.did = verifyDidSetup(ed25519.did, Curve.ED25519)
        ed25519.jwtSchema = verifyJwtSchemaSetupUrl(ed25519.jwtSchema.guid, ed25519.did)
        ed25519.credDefUrl = verifyAnoncredDefinitionUrl(ed25519.credDefUrl.guid, ed25519.did, ed25519.did)

        Notes.appendMessage("Agent: $agent")
        Notes.appendMessage("Mediator: $mediator")
        Notes.appendMessage("Secp256k1: $secp256k1")
        Notes.appendMessage("Ed25519: $ed25519")
    }

    private fun loadProperties(): Properties {
        val properties = Properties()
        val localProperties = this::class.java.classLoader.getResourceAsStream("local.properties")
        if (localProperties != null) {
            properties.load(localProperties)
        }
        properties.putAll(System.getenv())
        return properties
    }

    private fun configureRestAssured() {
        RestAssured.baseURI = agent.url
        if (agent.apiKey.isNotEmpty()) {
            val requestSpecification = RequestSpecBuilder()
                .addHeader("APIKEY", agent.apiKey)
                .build()
            SerenityRest.setDefaultRequestSpecification(requestSpecification)
        }
    }

    private fun verifyDidSetup(initialDid: String, curve: Curve): String {
        try {
            assertThat(initialDid).isNotEmpty()

            val response = RestAssured.given().get("dids/$initialDid").thenReturn()
            assertThat(response.statusCode).isEqualTo(200)

            val didDocument = response.body.jsonPath()

            val assertionMethods = didDocument.getList<String>("didDocument.assertionMethod")
            val authenticationMethods = didDocument.getList<String>("didDocument.authentication")

            val hasAssert1 = assertionMethods.any { it.contains("#assert1") }
            val hasAuth1 = authenticationMethods.any { it.contains("#auth1") }

            assertThat(hasAssert1).withFailMessage("Expected 'assert1' to be part of provided did").isTrue()
            assertThat(hasAuth1).withFailMessage("Expected 'auth1' to be part of provided did").isTrue()

            return initialDid
        } catch (e: AssertionError) {
            Notes.appendMessage("DID [$initialDid] not valid or not found. Creating a new one for $curve.")
        } catch (e: Exception) {
            Notes.appendMessage("Error checking DID [$initialDid]. Creating a new one for $curve.")
        }

        val creationData = CreateManagedDidRequest(
            documentTemplate = CreateManagedDidRequestDocumentTemplate(
                publicKeys = listOf(
                    ManagedDIDKeyTemplate("assert1", Purpose.ASSERTION_METHOD, curve),
                    ManagedDIDKeyTemplate("auth1", Purpose.AUTHENTICATION, curve)
                ),
                services = emptyList()
            )
        )

        val creationResponse = RestAssured.given()
            .body(creationData)
            .post("did-registrar/dids")
            .thenReturn()

        val longFormDid = creationResponse.body.jsonPath().getString("longFormDid")

        val publicationResponse = RestAssured.given()
            .post("did-registrar/dids/$longFormDid/publications")
            .thenReturn()

        assertThat(publicationResponse.statusCode).isEqualTo(HttpStatus.SC_ACCEPTED)
        val shortFormDid = publicationResponse.body.jsonPath().getString("scheduledOperation.didRef")

        lateinit var response: Response
        Wait.until(60.seconds, 1.seconds) {
            response = RestAssured.given()
                .get("did-registrar/dids/$shortFormDid")
                .thenReturn()
            response.body.jsonPath().getString("status") == "PUBLISHED"
        }

        return response.body.jsonPath().getString("did")
    }

    private fun verifyJwtSchemaSetupUrl(jwtSchemaGuid: String, did: String): JwtSchemaConfig {
        try {
            assertThat(jwtSchemaGuid).isNotEmpty()
            val response = RestAssured.given()
                .get("schema-registry/schemas/$jwtSchemaGuid")
                .thenReturn()

            assertThat(response.statusCode).isEqualTo(200)

            // Verify properties exist
            val properties = response.body.jsonPath().getMap<String, Any>("schema.properties")
            assertThat(properties).containsKey("automation-optional")
            assertThat(properties).containsKey("automation-required")

            val selfLink = response.body.jsonPath().getString("self")
            return JwtSchemaConfig(
                guid = jwtSchemaGuid,
                url = "${agent.url}$selfLink"
            )
        } catch (e: AssertionError) {
            Notes.appendMessage("JWT Schema [$jwtSchemaGuid] not valid or not found. Creating a new one.")
        }

        // Create new JWT Schema
        val schemaName = "automation-schema-" + UUID.randomUUID()
        val jwtSchema = JwtSchema().apply {
            id = "https://example.com/automated-credential"
            schema = "https://json-schema.org/draft/2020-12/schema"
            description = "automated-credential-schema"
            type = "object"
            properties["automation-required"] = JwtSchemaProperty("string")
            properties["automation-optional"] = JwtSchemaProperty("string")
            // Assuming the SDK model supports 'required' and 'additionalProperties',
            // otherwise these might need to be set differently depending on your SDK version
        }

        val credentialSchemaInput = CredentialSchemaInput(
            author = did,
            description = "Some description to automation generated schema",
            name = schemaName,
            tags = listOf("automation"),
            type = "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
            version = "1.0.0",
            schema = jwtSchema
        )

        val schemaResponse = RestAssured.given()
            .body(credentialSchemaInput)
            .post("schema-registry/schemas")
            .thenReturn()

        val newGuid = schemaResponse.body.jsonPath().getString("guid")
        val newSelf = schemaResponse.body.jsonPath().getString("self")

        // Note: Check if 'self' includes leading slash
        return JwtSchemaConfig(
            guid = newGuid,
            url = "${agent.url}$newSelf"
        )
    }

    private fun verifyAnoncredDefinitionUrl(definitionId: String, did: String, schemaDid: String): CredDefConfig {
        try {
            assertThat(definitionId).isNotEmpty()
            RestAssured.given()
                .get("credential-definition-registry/definitions/$definitionId")
                .then().assertThat().statusCode(200)
            return CredDefConfig(
                guid = definitionId,
                id = "${agent.url}/credential-definition-registry/definitions/$definitionId/definition"
            )
        } catch (e: AssertionError) {
            Notes.appendMessage("Anoncred Def [$definitionId] not found. Creating a new one.")
        }

        // 1. Create Schema First
        val schemaName = "automation-anoncred-schema-" + UUID.randomUUID()
        val anoncredSchema = AnoncredSchema().apply {
            name = "Automation Anoncred"
            version = "1.0"
            issuerId = schemaDid
            attrNames = mutableListOf("name", "age", "gender")
        }

        val credentialSchemaInput = CredentialSchemaInput(
            author = did,
            description = "Anoncred Schema for Kotlin",
            name = schemaName,
            tags = listOf("automation"),
            type = "AnoncredSchemaV1",
            version = "2.0.0",
            schema = anoncredSchema
        )

        val schemaCreationResponse = RestAssured.given()
            .body(credentialSchemaInput)
            .post("schema-registry/schemas")
            .thenReturn()

        val newSchemaGuid = schemaCreationResponse.body.jsonPath().getString("guid")

        // 2. Create Definition
        val definitionName = "automation-anoncred-definition-" + UUID.randomUUID()
        val definition = CredentialDefinitionInput(
            name = definitionName,
            version = "1.0.0",
            tag = "automation-test",
            author = did,
            schemaId = "${agent.url}/schema-registry/schemas/$newSchemaGuid/schema",
            signatureType = "CL",
            supportRevocation = false,
            description = "Test Automation Auto-Generated Kotlin"
        )

        val definitionResponse = RestAssured.given()
            .body(definition)
            .post("credential-definition-registry/definitions")
            .then().assertThat().statusCode(201)
            .extract().response()

        val newGuid = definitionResponse.body.jsonPath().getString("guid")

        return CredDefConfig(
            guid = newGuid,
            id = "${agent.url}/credential-definition-registry/definitions/$newGuid/definition"
        )
    }
}