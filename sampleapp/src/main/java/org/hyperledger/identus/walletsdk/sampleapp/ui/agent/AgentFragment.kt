package org.hyperledger.identus.walletsdk.ui.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.hyperledger.identus.walletsdk.sampleapp.databinding.FragmentAgentBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AgentFragment : Fragment() {

    private var _binding: FragmentAgentBinding? = null
    private val viewModel: AgentViewModel by viewModels()
    private var autoStartAttempted = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startAgent.setOnClickListener {
            val mediatorDID = binding.mediatorDid.text.toString()
            if (mediatorDID.isBlank()) {
                Toast.makeText(context, "Mediator DID is required", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.startAgent(mediatorDID)
        }

        viewModel.storedMediatorDid().observe(viewLifecycleOwner) { mediatorDID ->
            if (!autoStartAttempted && !mediatorDID.isNullOrBlank()) {
                autoStartAttempted = true
                binding.mediatorDid.setText(mediatorDID)
                viewModel.startAgent(mediatorDID)
            }
        }
        viewModel.startAgentError().observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(context, "Start agent failed: $error", Toast.LENGTH_LONG).show()
            }
        }
        viewModel.loadStoredMediatorDid()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(): AgentFragment {
            return AgentFragment()
        }
    }
}
