package io.iohk.atala.prism.walletsdk.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface JWTPayload {
    val iss: String
    val sub: String?
    val nbf: Long?
    val exp: Long?
    val jti: String?
    val aud: Array<String>?
    val originalJWTString: String?
    val verifiablePresentation: JWTVerifiablePresentation?
    val verifiableCredential: JWTVerifiableCredential?
}

/**
 * A struct representing the verifiable credential in a JWT credential payload.
 */
@Serializable
data class JWTVerifiableCredential @JvmOverloads constructor(
    @SerialName("@context")
    val context: Array<String> = arrayOf(),
    val type: Array<String> = arrayOf(),
    val credentialSchema: VerifiableCredentialTypeContainer? = null,
    val credentialSubject: Map<String, String>,
    val credentialStatus: VerifiableCredentialTypeContainer? = null,
    val refreshService: VerifiableCredentialTypeContainer? = null,
    val evidence: VerifiableCredentialTypeContainer? = null,
    val termsOfUse: VerifiableCredentialTypeContainer? = null
) {
    /**
     * Checks if this JWTVerifiableCredential object is equal to the specified object.
     *
     * @param other The object to compare this JWTVerifiableCredential object against.
     * @return true if the objects are equal, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as JWTVerifiableCredential

        if (!context.contentEquals(other.context)) return false
        if (!type.contentEquals(other.type)) return false
        if (credentialSchema != other.credentialSchema) return false
        if (credentialSubject != other.credentialSubject) return false
        if (credentialStatus != other.credentialStatus) return false
        if (refreshService != other.refreshService) return false
        if (evidence != other.evidence) return false
        if (termsOfUse != other.termsOfUse) return false

        return true
    }

    /**
     * Calculates the hash code value for the current object. The hash code is computed
     * based on the values of the object's properties.
     *
     * @return The hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = context.contentHashCode()
        result = 31 * result + type.contentHashCode()
        result = 31 * result + (credentialSchema?.hashCode() ?: 0)
        result = 31 * result + credentialSubject.hashCode()
        result = 31 * result + (credentialStatus?.hashCode() ?: 0)
        result = 31 * result + (refreshService?.hashCode() ?: 0)
        result = 31 * result + (evidence?.hashCode() ?: 0)
        result = 31 * result + (termsOfUse?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class JWTVerifiablePresentation(
    @SerialName("@context")
    val context: Array<String>,
    val type: Array<String>,
    val verifiableCredential: Array<String>
)
