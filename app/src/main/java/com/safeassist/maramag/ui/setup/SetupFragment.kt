package com.safeassist.maramag.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.databinding.FragmentSetupBinding
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import com.safeassist.maramag.viewmodel.SetupViewModel

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SetupViewModel by viewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCompleteSetup.setOnClickListener {
            val mobility = when (binding.rgSetupMobility.checkedRadioButtonId) {
                R.id.rb_setup_mobility -> MobilityProfile.MOBILITY
                R.id.rb_setup_senior -> MobilityProfile.SENIOR
                else -> MobilityProfile.GENERAL
            }
            viewModel.completeSetup(
                fullName = binding.etSetupName.text?.toString().orEmpty(),
                contactNumber = binding.etSetupPhone.text?.toString().orEmpty(),
                barangay = binding.etSetupBarangay.text?.toString().orEmpty(),
                purok = binding.etSetupPurok.text?.toString().orEmpty(),
                landmark = binding.etSetupLandmark.text?.toString().orEmpty(),
                mobilityProfile = mobility,
                assistanceNotes = binding.etSetupNotes.text?.toString().orEmpty()
            ) {
                findNavController().navigate(R.id.action_setup_to_home)
            }
        }

        binding.btnSkipSetup.setOnClickListener {
            viewModel.skipSetup {
                findNavController().navigate(R.id.action_setup_to_home)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
