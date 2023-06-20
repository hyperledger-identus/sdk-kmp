package io.iohk.atala.prism.walletsdk.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents a public key with a specific key curve and value.
 */
@Serializable
data class PublicKey(
    val curve: KeyCurve,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PublicKey

        if (curve != other.curve) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = curve.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}

/**
 * Represents a compressed public key and its uncompressed version.
 */
@Serializable
data class CompressedPublicKey(
    val uncompressed: PublicKey,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CompressedPublicKey

        if (uncompressed != other.uncompressed) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uncompressed.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}
