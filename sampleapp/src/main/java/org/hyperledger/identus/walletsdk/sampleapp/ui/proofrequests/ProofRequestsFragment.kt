package org.hyperledger.identus.walletsdk.sampleapp.ui.proofrequests

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hyperledger.identus.walletsdk.domain.models.Credential
import org.hyperledger.identus.walletsdk.domain.models.Message
import org.hyperledger.identus.walletsdk.pollux.models.AnonCredential
import org.hyperledger.identus.walletsdk.pollux.models.JWTCredential
import org.hyperledger.identus.walletsdk.pollux.models.SDJWTCredential
import org.hyperledger.identus.walletsdk.sampleapp.Sdk
import org.hyperledger.identus.walletsdk.sampleapp.databinding.CredentialDialogBinding
import org.hyperledger.identus.walletsdk.sampleapp.databinding.FragmentProofRequestsBinding
import org.hyperledger.identus.walletsdk.sampleapp.ui.messages.MessagesViewModel
import org.hyperledger.identus.walletsdk.ui.messages.CustomArrayAdapter

class ProofRequestsFragment : Fragment() {

    private var _binding: FragmentProofRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessagesViewModel by activityViewModels()
    private lateinit var adapter: ProofRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProofRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ProofRequestsAdapter { message ->
            fetchCredentialsAndShowDialog(message)
        }
        binding.proofRequestsList.adapter = adapter
        viewModel.pendingProofRequests().observe(viewLifecycleOwner) { requests ->
            adapter.updateRequests(requests)
        }
        viewModel.streamError().observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, "Send proof failed: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchCredentialsAndShowDialog(message: Message) {
        lifecycleScope.launch(Dispatchers.IO) {
            val allCredentials = Sdk.getInstance().agent.getAllCredentials().first()
            val credentials = filterCredentialsForRequest(allCredentials, message)
            withContext(Dispatchers.Main) {
                if (credentials.isEmpty()) {
                    Toast.makeText(context, "No credentials available", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                showDialogWithOptions(credentials, message)
            }
        }
    }

    private fun filterCredentialsForRequest(
        credentials: List<Credential>,
        message: Message
    ): List<Credential> {
        val format = message.attachments.firstOrNull()?.format.orEmpty().lowercase()
        return when {
            format.contains("sdjwt") -> {
                credentials.filterIsInstance<SDJWTCredential>()
            }
            format.contains("anoncred") -> {
                credentials.filterIsInstance<AnonCredential>()
            }
            format.contains("jwt") -> {
                credentials.filterIsInstance<JWTCredential>()
            }
            else -> credentials
        }
    }

    private fun showDialogWithOptions(
        credentials: List<Credential>,
        message: Message
    ) {
        val dialogBinding = CredentialDialogBinding.inflate(layoutInflater)
        context?.let {
            val adapter =
                CustomArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, credentials)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinner.adapter = adapter

            AlertDialog.Builder(context)
                .setTitle("Choose an Option")
                .setView(dialogBinding.root)
                .setPositiveButton("OK") { _, _ ->
                    val credential = credentials[dialogBinding.spinner.selectedItemPosition]
                    try {
                        if (credential is SDJWTCredential) {
                            showDisclosureDialog(credential, message)
                        } else {
                            viewModel.preparePresentationProof(credential, message)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Send proof failed: ${e.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun showDisclosureDialog(credential: SDJWTCredential, message: Message) {
        val disclosureKeys = credential.disclosureClaimKeys()
        val claimKeys = if (disclosureKeys.isNotEmpty()) {
            disclosureKeys.distinct().sorted()
        } else {
            credential.claims.map { it.key }.sorted()
        }
        if (claimKeys.isEmpty()) {
            Toast.makeText(context, "No claims available to disclose", Toast.LENGTH_LONG).show()
            return
        }
        val checked = BooleanArray(claimKeys.size) { true }
        AlertDialog.Builder(context)
            .setTitle("Select claims to disclose")
            .setMultiChoiceItems(claimKeys.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = claimKeys.filterIndexed { index, _ -> checked[index] }
                if (selected.isEmpty()) {
                    Toast.makeText(context, "Select at least one claim", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                viewModel.preparePresentationProof(credential, message, selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        @JvmStatic
        fun newInstance(): ProofRequestsFragment {
            return ProofRequestsFragment()
        }
    }
}
