package io.iohk.atala.prism.walletsdk.pollux.models

import io.iohk.atala.prism.apollo.base64.base64UrlDecoded
import io.iohk.atala.prism.didcomm.didpeer.core.toJsonElement
import io.iohk.atala.prism.walletsdk.domain.VC
import io.iohk.atala.prism.walletsdk.domain.models.Claim
import io.iohk.atala.prism.walletsdk.domain.models.ClaimType
import io.iohk.atala.prism.walletsdk.domain.models.Credential
import io.iohk.atala.prism.walletsdk.domain.models.JWTPayload
import io.iohk.atala.prism.walletsdk.domain.models.JWTVerifiableCredential
import io.iohk.atala.prism.walletsdk.domain.models.JWTVerifiablePresentation
import io.iohk.atala.prism.walletsdk.domain.models.StorableCredential
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
/**
 * Represents a JSON Web Token (JWT) credential.
 *
 * This class provides a way to parse and extract information from a JWT string.
 * It implements the Credential interface and provides implementations for all its properties and functions.
 *
 * @property data The original JWT string representation of the credential.
 * @property jwtString The JWT string representation of the credential.
 * @property jwtPayload The parsed JWT payload containing the credential information.
 */
@OptIn(ExperimentalSerializationApi::class)
data class JWTCredential(
    override val id: String,
    override val iss: String,
    override val sub: String?,
    override val nbf: Long?,
    override val exp: Long?,
    override val jti: String?,
    @Serializable(with = AudSerializer::class)
    override val aud: Array<String>?,
    override val originalJWTString: String?,
    @SerialName("vp")
    override var verifiablePresentation: JWTVerifiablePresentation? = null,
    @SerialName(VC)
    override var verifiableCredential: JWTVerifiableCredential? = null
) : Credential, JWTPayload {

    @Transient
    override val issuer: String = iss

    override val subject: String?
        get() = sub

    override val claims: Array<Claim>
        get() {
            return verifiableCredential?.credentialSubject?.map {
                Claim(key = it.key, value = ClaimType.StringValue(it.value))
            }?.toTypedArray()
                ?: emptyArray<Claim>()
        }

    override val properties: Map<String, Any?>
        get() {
            val properties = mutableMapOf<String, Any?>()
            properties["nbf"] = nbf
            properties["jti"] = jti
            verifiableCredential?.let { verifiableCredential ->
                properties["type"] = verifiableCredential.type
                verifiableCredential.credentialSchema?.let {
                    properties["schema"] = it.id
                }
                verifiableCredential.credentialStatus?.let {
                    properties["credentialStatus"] = it.type
                }
                verifiableCredential.refreshService?.let {
                    properties["refreshService"] = it.type
                }
                verifiableCredential.evidence?.let {
                    properties["evidence"] = it.type
                }
                verifiableCredential.termsOfUse?.let {
                    properties["termsOfUse"] = it.type
                }
            }
            verifiablePresentation?.let { verifiablePresentation ->
                properties["type"] = verifiablePresentation.type
            }
            properties["aud"] = aud
            properties["id"] = id

            exp?.let { properties["exp"] = it }
            return properties.toMap()
        }

    override var revoked: Boolean? = null

    /**
     * Converts the current instance of [JWTCredential] to a [StorableCredential].
     *
     * @return The converted [StorableCredential].
     */
    fun toStorableCredential(): StorableCredential {
        val c = this
        return object : StorableCredential {
            override val id: String
                get() = c.id
            override val recoveryId: String
                get() = "jwt+credential"
            override val credentialData: ByteArray
                get() = c.id.toByteArray()

            override val issuer: String
                get() = c.issuer

            override val subject: String?
                get() = c.subject
            override val credentialCreated: String?
                get() = null
            override val credentialUpdated: String?
                get() = null
            override val credentialSchema: String?
                get() = verifiableCredential?.credentialSchema?.type
            override val validUntil: String?
                get() = null
            override var revoked: Boolean? = c.revoked
            override val availableClaims: Array<String>
                get() = c.claims.map { it.key }.toTypedArray()

            override val claims: Array<Claim>
                get() = verifiableCredential?.credentialSubject?.map {
                    Claim(key = it.key, value = ClaimType.StringValue(it.value))
                }?.toTypedArray() ?: emptyArray()

            override val properties: Map<String, Any?>
                get() {
                    val properties = mutableMapOf<String, Any?>()
                    properties["nbf"] = nbf
                    properties["jti"] = jti
                    properties["aud"] = aud
                    properties["id"] = id

                    exp?.let { properties["exp"] = it }
                    verifiableCredential?.let { verifiableCredential ->
                        properties["type"] = verifiableCredential.type
                        verifiableCredential.credentialSchema?.let {
                            properties["schema"] = it.id
                        }
                        verifiableCredential.credentialStatus?.let {
                            properties["credentialStatus"] = it.type
                        }
                        verifiableCredential.refreshService?.let {
                            properties["refreshService"] = it.type
                        }
                        verifiableCredential.evidence?.let {
                            properties["evidence"] = it.type
                        }
                        verifiableCredential.termsOfUse?.let {
                            properties["termsOfUse"] = it.type
                        }
                    }
                    verifiablePresentation?.let { verifiablePresentation ->
                        properties["type"] = verifiablePresentation.type
                    }

                    return properties.toMap()
                }

            /**
             * Converts the current instance of [JWTCredential] to a [Credential].
             *
             * @return The converted [Credential].
             */
            override fun fromStorableCredential(): Credential {
                return c
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    object AudSerializer : JsonTransformingSerializer<Array<String>>(ArraySerializer(String.serializer())) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            // Check if the element is a JSON array
            if (element is JsonArray) {
                return element
            }
            // If it's a single string, wrap it into an array
            return Json.encodeToJsonElement(arrayOf(element.jsonPrimitive.content))
        }
    }

    companion object {
        @JvmStatic
        fun fromJwtString(jwtString: String): JWTCredential {
            val jwtParts = jwtString.split(".")
            require(jwtParts.size == 3) { "Invalid JWT string" }
            val credentialString = jwtParts[1]
            val jsonString = credentialString.base64UrlDecoded

            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

            val jsonObject = Json.decodeFromString<JsonElement>(jsonString).jsonObject
            return json.decodeFromJsonElement(jsonObject.plus("id" to jwtString).toJsonElement())
        }
    }
}
