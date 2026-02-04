package org.hyperledger.identus.walletsdk.ui.messages

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.hyperledger.identus.walletsdk.sampleapp.databinding.FragmentMessagesBinding
import org.hyperledger.identus.walletsdk.sampleapp.ui.messages.InitiateVerificationDialogFragment
import org.hyperledger.identus.walletsdk.sampleapp.ui.messages.MessagesAdapter
import org.hyperledger.identus.walletsdk.sampleapp.ui.messages.MessagesViewModel
import org.hyperledger.identus.walletsdk.sampleapp.ui.messages.UiMessage

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val viewModel: MessagesViewModel by activityViewModels()

    private val binding get() = _binding!!

    interface ValidateMessageListener {
        fun validateMessage(message: UiMessage)
    }

    private lateinit var adapter: MessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendMessage.setOnClickListener {
            viewModel.sendMessage()
        }
        binding.sendVerification.setOnClickListener {
            InitiateVerificationDialogFragment(viewModel).show(
                parentFragmentManager,
                "InitiateVerificationDialogFragment"
            )
        }

        adapter = MessagesAdapter(
            validateListener = object : ValidateMessageListener {
                override fun validateMessage(message: UiMessage) {
                    viewModel.handlePresentation(message).observe(viewLifecycleOwner) { status ->
                        adapter.updateMessageStatus(
                            UiMessage(
                                id = message.id,
                                piuri = message.piuri,
                                from = message.from,
                                to = message.to,
                                attachments = message.attachments,
                                status = status
                            )
                        )
                    }
                }
            }
        )
        binding.list.adapter = adapter
        setupStreamObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupStreamObservers() {
        viewModel.messagesStream().observe(this.viewLifecycleOwner) { messages ->
            val uiMessages = messages.map { message ->
                UiMessage(
                    id = message.id,
                    piuri = message.piuri,
                    from = message.from?.toString() ?: "NA",
                    to = message.to?.toString() ?: "NA",
                    attachments = message.attachments
                )
            }
            adapter.updateMessages(uiMessages)
        }
        viewModel.revokedCredentialsStream()
            .observe(this.viewLifecycleOwner) { revokedCredentials ->
                if (revokedCredentials.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Credential revoked ID: ${revokedCredentials.last().id}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        viewModel.streamError()
            .observe(this.viewLifecycleOwner) { error ->
                AlertDialog.Builder(context)
                    .setTitle("An error occurred")
                    .setMessage(error)
                    .setNeutralButton("Ok") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .show()
            }
    }

    companion object {
        @JvmStatic
        fun newInstance(): MessagesFragment {
            return MessagesFragment()
        }
    }

}
