package io.iohk.atala.prism.walletsdk.domain.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

interface AttachmentData

@Serializable
data class AttachmentHeader(
    val children: String
) : AttachmentData

@Serializable
data class AttachmentJws(
    val header: AttachmentHeader,
    val protected: String,
    val signature: String
) : AttachmentData

@Serializable
data class AttachmentJwsData(
    val base64: String,
    val jws: AttachmentJws
) : AttachmentData

@Serializable
data class AttachmentBase64(
    val base64: String
) : AttachmentData

@Serializable
data class AttachmentLinkData(
    val links: Array<String>,
    val hash: String
) : AttachmentData {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AttachmentLinkData

        if (!links.contentEquals(other.links)) return false
        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = links.contentHashCode()
        result = 31 * result + hash.hashCode()
        return result
    }
}

@Serializable
data class AttachmentJsonData(
    val data: String
) : AttachmentData

@Serializable
data class AttachmentDescriptor @JvmOverloads constructor(
    val id: String,
    val mediaType: String? = null,
    val data: AttachmentData,
    val filename: Array<String>? = null,
    val format: String? = null,
    val lastModTime: String? = null, // TODO(Date format)
    val byteCount: Int? = null,
    val description: String? = null
) : AttachmentData {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AttachmentDescriptor

        if (id != other.id) return false
        if (mediaType != other.mediaType) return false
        if (data != other.data) return false
        if (!filename.contentEquals(other.filename)) return false
        if (format != other.format) return false
        if (lastModTime != other.lastModTime) return false
        if (byteCount != other.byteCount) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (mediaType?.hashCode() ?: 0)
        result = 31 * result + data.hashCode()
        result = 31 * result + filename.contentHashCode()
        result = 31 * result + (format?.hashCode() ?: 0)
        result = 31 * result + (lastModTime?.hashCode() ?: 0)
        result = 31 * result + (byteCount ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}
