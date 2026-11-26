package com.safeassist.maramag.ui.sos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.communication.CommunicationHelper
import com.safeassist.maramag.databinding.FragmentSosCountdownBinding
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.navigation.SafeAssistNavigator
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import com.safeassist.maramag.viewmodel.SosViewModel
import kotlinx.coroutines.launch

class SosCountdownFragment : Fragment() {

    private var _binding: FragmentSosCountdownBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SosViewModel by activityViewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.startSosCountdown()
        }

        val cancelAction = {
            viewModel.cancelSos()
            SafeAssistNavigator.navigateHomeFromSos(findNavController())
        }

        binding.btnCancelSos.setOnClickListener { cancelAction() }

        binding.btnStopSirenCountdown.setOnClickListener {
            viewModel.stopSiren()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cancelAction()
                }
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.secondsRemaining.collect { sec ->
                        binding.tvCountdownNumber.text = sec.toString()
                    }
                }
                launch {
                    viewModel.isSirenPlaying.collect { playing ->
                        binding.btnStopSirenCountdown.visibility = if (playing) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.countdownFinished.collect { finished ->
                        if (finished) {
                            viewModel.consumeCountdownFinished()
                            binding.tvCountdownHeader.text = "Opening Messages"
                            binding.tvCountdownSubtitle.text = "ready"
                            binding.btnCancelSos.text = "Back to Home"
                            val contacts = viewModel.emergencyContacts.value
                            val recipients = if (contacts.isNotEmpty()) {
                                contacts.map { it.phoneNumber }
                            } else {
                                listOf(viewModel.appSettings.value.mdrrmoHotline)
                            }
                            val message = viewModel.buildSosMessage()
                            val res = CommunicationHelper.openSmsApp(requireContext(), recipients, message)
                            if (res is CommunicationHelper.ActionResult.Error) {
                                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                launch {
                    viewModel.userProfile.collect { profile ->
                        binding.tvAttachedLocation.text = "Where: ${profile.formattedLocation()}"
                        val notes = buildList {
                            if (profile.mobilityProfile != MobilityProfile.GENERAL) {
                                add(profile.mobilityProfile.displayName)
                            }
                            if (profile.assistanceNotes.isNotBlank()) {
                                add(profile.assistanceNotes)
                            }
                        }
                        if (notes.isEmpty()) {
                            binding.tvAttachedNeeds.visibility = View.GONE
                        } else {
                            binding.tvAttachedNeeds.visibility = View.VISIBLE
                            binding.tvAttachedNeeds.text = "Needs: ${notes.joinToString(" | ")}"
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
