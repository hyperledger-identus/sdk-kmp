package org.hyperledger.identus.walletsdk.sampleapp.ui.proofrequests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hyperledger.identus.walletsdk.domain.models.AttachmentDescriptor
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.sampleapp.R

class ProofRequestsAdapter(
    private var data: MutableList<Message> = mutableListOf(),
    private val sendProofListener: (Message) -> Unit
) : RecyclerView.Adapter<ProofRequestsAdapter.ProofRequestHolder>() {

    fun updateRequests(updatedRequests: List<Message>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = data.size

            override fun getNewListSize(): Int = updatedRequests.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return data[oldItemPosition].id == updatedRequests[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return data[oldItemPosition] == updatedRequests[newItemPosition]
            }
        })
        data.clear()
        data.addAll(updatedRequests)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProofRequestHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_proof_request, parent, false)
        return ProofRequestHolder(view)
    }

    override fun onBindViewHolder(holder: ProofRequestHolder, position: Int) {
        holder.bind(data[position], sendProofListener)
    }

    override fun getItemCount(): Int = data.size

    inner class ProofRequestHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.proof_request_title)
        private val type: TextView = itemView.findViewById(R.id.proof_request_type)
        private val claims: TextView = itemView.findViewById(R.id.proof_request_claims)
        private val issuer: TextView = itemView.findViewById(R.id.proof_request_issuer)
        private val verifier: TextView = itemView.findViewById(R.id.proof_request_verifier)
        private val sendProof: Button = itemView.findViewById(R.id.send_proof)

        fun bind(message: Message, sendProofListener: (Message) -> Unit) {
            val details = extractDetails(message)
            title.text = "Present-proof request"
            type.text = "Type: ${details.requestedType}"
            claims.text = "Claims: ${details.requestedClaims}"
            issuer.text = "Issuer: ${details.requestedIssuer}"
            verifier.text = "From: ${details.verifierDid}"
            sendProof.setOnClickListener { sendProofListener(message) }
        }
    }

    private data class RequestDetails(
        val requestedType: String,
        val requestedClaims: String,
        val requestedIssuer: String,
        val verifierDid: String
    )

    private fun extractDetails(message: Message): RequestDetails {
        val attachment = message.attachments.firstOrNull()
        val verifier = message.from?.toString() ?: "NA"
        val json = attachment?.let { parseAttachmentJson(it) }

        val type = extractType(attachment, json)
        val claims = extractClaims(json)
        val issuer = extractIssuer(json)

        return RequestDetails(
            requestedType = type,
            requestedClaims = claims,
            requestedIssuer = issuer,
            verifierDid = verifier
        )
    }

    private fun parseAttachmentJson(attachment: AttachmentDescriptor) =
        runCatching {
            val data = attachment.data.getDataAsJsonString()
            Json.parseToJsonElement(data).jsonObject
        }.getOrNull()

    private fun extractType(attachment: AttachmentDescriptor?, json: kotlinx.serialization.json.JsonObject?): String {
        val explicit = json?.get("credentialFormat")?.jsonPrimitive?.content
        if (!explicit.isNullOrBlank()) {
            return explicit.uppercase()
        }
        val format = attachment?.format.orEmpty().lowercase()
        val formatMap = json
            ?.get("presentation_definition")
            ?.jsonObject
            ?.get("format")
            ?.jsonObject
        if (formatMap != null) {
            val key = formatMap.keys.firstOrNull()
            if (!key.isNullOrBlank()) return key.uppercase()
        }
        return when {
            format.contains("sd-jwt") -> "SDJWT"
            format.contains("anoncred") -> "ANONCREDS"
            format.contains("jwt") -> "JWT"
            else -> "UNKNOWN"
        }
    }

    private fun extractClaims(json: kotlinx.serialization.json.JsonObject?): String {
        val claimsObj = json?.get("claims")?.jsonObject
        val claimKeys = claimsObj?.keys?.sorted()
        return if (claimKeys.isNullOrEmpty()) "-" else claimKeys.joinToString(", ")
    }

    private fun extractIssuer(json: kotlinx.serialization.json.JsonObject?): String {
        val issuer = json
            ?.get("proofs")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("trustIssuers")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonPrimitive
            ?.content
        return issuer ?: "-"
    }
}
