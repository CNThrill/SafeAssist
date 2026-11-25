package com.safeassist.maramag.ui.prepare

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.databinding.FragmentWhereAndNeedsBinding
import com.safeassist.maramag.model.AssistanceNeed
import com.safeassist.maramag.model.IncidentType
import com.safeassist.maramag.model.WhoNeedsHelp
import com.safeassist.maramag.navigation.SafeAssistNavigator
import com.safeassist.maramag.viewmodel.PrepareEmergencyViewModel
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import kotlinx.coroutines.launch

class WhereAndNeedsFragment : Fragment() {

    private var _binding: FragmentWhereAndNeedsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrepareEmergencyViewModel by activityViewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    private var isPopulating = false
    private var hasPopulatedTextFields = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhereAndNeedsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.startNewOrResume()

        binding.btnBackHome.setOnClickListener {
            saveTextFieldsToDraft()
            findNavController().popBackStack()
        }

        binding.btnLocationNotSure.setOnClickListener {
            if (binding.etReportBarangay.text.isNullOrBlank()) {
                binding.etReportBarangay.setText("Unknown")
            }
            if (binding.etReportLandmark.text.isNullOrBlank()) {
                binding.etReportLandmark.setText("Unknown")
            }
            saveTextFieldsToDraft()
        }

        binding.chipGroupWho.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isPopulating) return@setOnCheckedStateChangeListener
            val who = when (checkedIds.firstOrNull()) {
                R.id.chip_who_family -> WhoNeedsHelp.FAMILY_MEMBER
                R.id.chip_who_someone_else -> WhoNeedsHelp.SOMEONE_ELSE
                else -> WhoNeedsHelp.ME
            }
            viewModel.updateWho(who)
            binding.tilPersonName.visibility =
                if (who == WhoNeedsHelp.ME) View.GONE else View.VISIBLE
        }

        binding.rgWhatHappened.setOnCheckedChangeListener { _, checkedId ->
            if (isPopulating) return@setOnCheckedChangeListener
            val type = when (checkedId) {
                R.id.rb_incident_flood -> IncidentType.FLOOD
                R.id.rb_incident_fire -> IncidentType.FIRE
                R.id.rb_incident_landslide -> IncidentType.LANDSLIDE
                R.id.rb_incident_other -> IncidentType.OTHER
                else -> IncidentType.MEDICAL
            }
            viewModel.updateIncidentType(type)
        }

        val needChipMap = mapOf(
            binding.chipNeedMedical to AssistanceNeed.MEDICAL_CARE,
            binding.chipNeedRescue to AssistanceNeed.RESCUE,
            binding.chipNeedEvac to AssistanceNeed.EVACUATION,
            binding.chipNeedFood to AssistanceNeed.FOOD_AND_WATER,
            binding.chipNeedMobility to AssistanceNeed.MOBILITY_SUPPORT
        )
        needChipMap.forEach { (chip, need) ->
            chip.setOnCheckedChangeListener { _, _ ->
                if (!isPopulating) {
                    viewModel.toggleAssistanceNeed(need)
                }
            }
        }

        binding.etExtraDetails.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                binding.tvExtraCharCounter.text = "$len/120"
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isPopulating) {
                    viewModel.updateExtraDetails(s?.toString().orEmpty())
                }
            }
        })

        binding.btnReviewDetails.setOnClickListener {
            saveTextFieldsToDraft()
            SafeAssistNavigator.navigateToReviewAndShare(findNavController())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isDraftReady.collect { ready ->
                        if (ready && !hasPopulatedTextFields) {
                            hasPopulatedTextFields = true
                            val draft = viewModel.draft.value
                            isPopulating = true
                            binding.etPersonName.setText(draft.personName)
                            binding.etReportBarangay.setText(draft.barangay)
                            binding.etReportPurok.setText(draft.purok)
                            binding.etReportStreet.setText(draft.street)
                            binding.etReportLandmark.setText(draft.landmark)
                            binding.etReportHousehold.setText(draft.householdDescription)
                            binding.etExtraDetails.setText(draft.extraDetails)
                            isPopulating = false
                        }
                    }
                }
                launch {
                    viewModel.draft.collect { draft ->
                        isPopulating = true
                        when (draft.whoNeedsHelp) {
                            WhoNeedsHelp.ME -> binding.chipWhoMe.isChecked = true
                            WhoNeedsHelp.FAMILY_MEMBER -> binding.chipWhoFamily.isChecked = true
                            WhoNeedsHelp.SOMEONE_ELSE -> binding.chipWhoSomeoneElse.isChecked = true
                        }
                        binding.tilPersonName.visibility =
                            if (draft.whoNeedsHelp == WhoNeedsHelp.ME) View.GONE else View.VISIBLE

                        val radioId = when (draft.incidentType) {
                            IncidentType.MEDICAL -> R.id.rb_incident_medical
                            IncidentType.FLOOD -> R.id.rb_incident_flood
                            IncidentType.FIRE -> R.id.rb_incident_fire
                            IncidentType.LANDSLIDE -> R.id.rb_incident_landslide
                            IncidentType.OTHER -> R.id.rb_incident_other
                        }
                        binding.rgWhatHappened.check(radioId)

                        binding.chipNeedMedical.isChecked = draft.assistanceNeeds.contains(AssistanceNeed.MEDICAL_CARE)
                        binding.chipNeedRescue.isChecked = draft.assistanceNeeds.contains(AssistanceNeed.RESCUE)
                        binding.chipNeedEvac.isChecked = draft.assistanceNeeds.contains(AssistanceNeed.EVACUATION)
                        binding.chipNeedFood.isChecked = draft.assistanceNeeds.contains(AssistanceNeed.FOOD_AND_WATER)
                        binding.chipNeedMobility.isChecked = draft.assistanceNeeds.contains(AssistanceNeed.MOBILITY_SUPPORT)

                        binding.tvExtraCharCounter.text = "${draft.extraDetails.length}/120"
                        isPopulating = false
                    }
                }
            }
        }
    }

    private fun saveTextFieldsToDraft() {
        viewModel.updatePersonName(binding.etPersonName.text?.toString().orEmpty())
        viewModel.updateLocation(
            barangay = binding.etReportBarangay.text?.toString().orEmpty(),
            purok = binding.etReportPurok.text?.toString().orEmpty(),
            street = binding.etReportStreet.text?.toString().orEmpty(),
            landmark = binding.etReportLandmark.text?.toString().orEmpty(),
            householdDescription = binding.etReportHousehold.text?.toString().orEmpty()
        )
        viewModel.updateExtraDetails(binding.etExtraDetails.text?.toString().orEmpty())
    }

    override fun onPause() {
        super.onPause()
        saveTextFieldsToDraft()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hasPopulatedTextFields = false
        _binding = null
    }
}
