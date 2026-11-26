package com.safeassist.maramag.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.databinding.FragmentProfileBinding
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.viewmodel.ProfileViewModel
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    private var hasPopulatedFields = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barangays = resources.getStringArray(R.array.maramag_barangays)
        val barangayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            barangays
        )
        binding.etBarangay.setAdapter(barangayAdapter)

        val puroks = resources.getStringArray(R.array.maramag_puroks)
        val purokAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            puroks
        )
        binding.etPurok.setAdapter(purokAdapter)

        // Populate once after Room emits the real profile so returning from background doesn't wipe edits
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.filterNotNull().collect { profile ->
                    if (!hasPopulatedFields) {
                        hasPopulatedFields = true
                        binding.etFullName.setText(profile.fullName)
                        binding.etContactNumber.setText(profile.contactNumber)
                        binding.etBarangay.setText(profile.barangay, false)
                        binding.etPurok.setText(profile.purok, false)
                        binding.etStreetHousehold.setText(profile.streetOrHousehold)
                        binding.etLandmark.setText(profile.landmark)
                        binding.etAssistanceNotes.setText(profile.assistanceNotes)

                        val radioId = when (profile.mobilityProfile) {
                            MobilityProfile.MOBILITY -> R.id.rb_mobility
                            MobilityProfile.SENIOR -> R.id.rb_senior
                            MobilityProfile.GENERAL -> R.id.rb_general
                            MobilityProfile.OTHER -> R.id.rb_other_profile
                        }
                        binding.rgMobilityProfile.check(radioId)
                    }
                }
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            val selectedProfile = when (binding.rgMobilityProfile.checkedRadioButtonId) {
                R.id.rb_mobility -> MobilityProfile.MOBILITY
                R.id.rb_senior -> MobilityProfile.SENIOR
                R.id.rb_other_profile -> MobilityProfile.OTHER
                else -> MobilityProfile.GENERAL
            }

            viewModel.saveProfile(
                fullName = binding.etFullName.text?.toString().orEmpty(),
                contactNumber = binding.etContactNumber.text?.toString().orEmpty(),
                barangay = binding.etBarangay.text?.toString().orEmpty(),
                purok = binding.etPurok.text?.toString().orEmpty(),
                streetOrHousehold = binding.etStreetHousehold.text?.toString().orEmpty(),
                landmark = binding.etLandmark.text?.toString().orEmpty(),
                mobilityProfile = selectedProfile,
                assistanceNotes = binding.etAssistanceNotes.text?.toString().orEmpty()
            ) {
                Toast.makeText(
                    requireContext(),
                    "My info saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasPopulatedFields = false
        _binding = null
    }
}
