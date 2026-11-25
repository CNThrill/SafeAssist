package com.safeassist.maramag.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.communication.CommunicationHelper
import com.safeassist.maramag.databinding.FragmentHomeBinding
import com.safeassist.maramag.navigation.SafeAssistNavigator
import com.safeassist.maramag.viewmodel.HomeViewModel
import com.safeassist.maramag.viewmodel.PrepareEmergencyViewModel
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    private val prepareViewModel: PrepareEmergencyViewModel by activityViewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardCaregiverMode.setOnClickListener {
            viewModel.toggleCaregiverMode()
        }

        binding.btnSwitchMode.setOnClickListener {
            viewModel.toggleCaregiverMode()
        }

        binding.btnSos.setOnClickListener {
            SafeAssistNavigator.navigateToSos(findNavController())
        }

        binding.btnPrepareDetails.setOnClickListener {
            prepareViewModel.resetDraft()
            SafeAssistNavigator.navigateToWhereAndNeeds(findNavController())
        }

        binding.cardQuickMdrrmo.setOnClickListener {
            val hotline = viewModel.appSettings.value.mdrrmoHotline
            val result = CommunicationHelper.openDialer(requireContext(), hotline)
            if (result is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.cardQuickTrusted.setOnClickListener {
            val contacts = viewModel.emergencyContacts.value
            val primary = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
            if (primary != null) {
                val result = CommunicationHelper.openDialer(requireContext(), primary.phoneNumber)
                if (result is CommunicationHelper.ActionResult.Error) {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            } else {
                findNavController().navigate(R.id.action_home_to_contacts)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.appSettings.collect { settings ->
                        binding.tvCaregiverStatus.text = if (settings.caregiverModeEnabled) {
                            getString(R.string.helping_someone)
                        } else {
                            getString(R.string.helping_myself)
                        }
                        binding.tvQuickMdrrmoNumber.text = settings.mdrrmoHotline
                    }
                }
                launch {
                    viewModel.emergencyContacts.collect { contacts ->
                        binding.tvQuickTrustedSub.text = if (contacts.isEmpty()) {
                            "Add in Contacts"
                        } else {
                            val first = contacts.firstOrNull { it.isPrimary } ?: contacts.first()
                            first.name
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
