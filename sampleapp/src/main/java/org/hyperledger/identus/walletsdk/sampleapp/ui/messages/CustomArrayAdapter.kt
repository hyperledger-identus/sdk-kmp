package org.hyperledger.identus.walletsdk.ui.messages

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.hyperledger.identus.walletsdk.domain.models.Credential
import org.hyperledger.identus.walletsdk.pollux.models.AnonCredential
import org.hyperledger.identus.walletsdk.pollux.models.JWTCredential
import org.hyperledger.identus.walletsdk.pollux.models.SDJWTCredential

class CustomArrayAdapter(
    context: Context,
    resource: Int,
    objects: List<Credential>
) : ArrayAdapter<Credential>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        val credential = getItem(position)
        credential?.let {
            label.text = formatCredentialLabel(it)
        }
        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        val credential = getItem(position)
        credential?.let {
            label.text = formatCredentialLabel(it)
        }
        return label
    }

    private fun formatCredentialLabel(credential: Credential): String {
        return when (credential) {
            is JWTCredential -> {
                val schemaUrl = credential.properties["schema"]?.toString() ?: ""
                val schemaName = extractSchemaName(schemaUrl)
                val issuer = extractShortDid(credential.issuer)
                val claims = credential.claims.take(2).joinToString(", ") { it.key }
                "JWT: $schemaName\nIssuer: $issuer\nClaims: $claims"
            }
            is AnonCredential -> {
                val schemaName = extractSchemaName(credential.schemaID)
                val claims = credential.claims.take(2).joinToString(", ") { it.key }
                "AnonCred: $schemaName\nClaims: $claims"
            }
            is SDJWTCredential -> {
                val disclosureKeys = credential.disclosureClaimKeys()
                val claims = disclosureKeys.take(2).joinToString(", ")
                "SD-JWT\nDisclosures: $claims"
            }
            else -> {
                "Credential: ${credential.id.take(16)}..."
            }
        }
    }

    private fun extractSchemaName(url: String): String {
        if (url.isBlank()) return "unknown"
        // Extract meaningful part from schema URL
        // e.g., "http://localhost:8085/schema-registry/schemas/abc123/schema" -> "abc123"
        return try {
            val parts = url.split("/")
            val schemaIndex = parts.indexOf("schemas")
            if (schemaIndex >= 0 && schemaIndex + 1 < parts.size) {
                parts[schemaIndex + 1].take(12) + if (parts[schemaIndex + 1].length > 12) "..." else ""
            } else {
                url.substringAfterLast("/").take(16) + if (url.substringAfterLast("/").length > 16) "..." else ""
            }
        } catch (e: Exception) {
            url.take(20) + "..."
        }
    }

    private fun extractShortDid(did: String): String {
        // e.g., "did:prism:abc123def456..." -> "did:prism:abc123..."
        return try {
            val parts = did.split(":")
            if (parts.size >= 3) {
                "${parts[0]}:${parts[1]}:${parts[2].take(8)}..."
            } else {
                did.take(20) + "..."
            }
        } catch (e: Exception) {
            did.take(20) + "..."
        }
    }
}
