package com.safeassist.maramag.ui.prepare

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.communication.CommunicationHelper
import com.safeassist.maramag.databinding.FragmentReviewAndShareBinding
import com.safeassist.maramag.model.WhoNeedsHelp
import com.safeassist.maramag.utils.DateTimeUtils
import com.safeassist.maramag.viewmodel.PrepareEmergencyViewModel
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar

class ReviewAndShareFragment : Fragment() {

    private var _binding: FragmentReviewAndShareBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrepareEmergencyViewModel by activityViewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewAndShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackToForm.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditSummary.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditTimestamp.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnCopySummaryText.setOnClickListener {
            val text = viewModel.buildFormattedSummary()
            when (val res = CommunicationHelper.copyToClipboard(requireContext(), "SafeAssist Emergency Summary", text)) {
                is CommunicationHelper.ActionResult.Success ->
                    Toast.makeText(
                        requireContext(),
                        "Message copied.",
                        Toast.LENGTH_LONG
                    ).show()
                is CommunicationHelper.ActionResult.Error ->
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSmsMdrrmoReview.setOnClickListener {
            val hotline = viewModel.appSettings.value.mdrrmoHotline
            val message = viewModel.buildFormattedSummary()
            val res = CommunicationHelper.openSmsApp(requireContext(), hotline, message)
            if (res is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSmsContactReview.setOnClickListener {
            val contacts = viewModel.emergencyContacts.value
            val message = viewModel.buildFormattedSummary()
            if (contacts.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "No saved contacts yet. Opening Messages.",
                    Toast.LENGTH_SHORT
                ).show()
                val res = CommunicationHelper.openSmsApp(requireContext(), "", message)
                if (res is CommunicationHelper.ActionResult.Error) {
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                }
            } else {
                val labels = contacts.map { "${it.name} • ${it.phoneNumber}" }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Send Text to Someone Else")
                    .setItems(labels) { _, which ->
                        val selected = contacts[which]
                        val res = CommunicationHelper.openSmsApp(requireContext(), selected.phoneNumber, message)
                        if (res is CommunicationHelper.ActionResult.Error) {
                            Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        binding.btnCallMdrrmoReview.setOnClickListener {
            val hotline = viewModel.appSettings.value.mdrrmoHotline
            val res = CommunicationHelper.openDialer(requireContext(), hotline)
            if (res is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.draft.collect { draft ->
                        renderWhoSummary(draft.whoNeedsHelp, draft.personName, viewModel.userProfile.value.fullName)

                        val whereParts = listOfNotNull(
                            draft.barangay.takeIf { it.isNotBlank() },
                            draft.purok.takeIf { it.isNotBlank() },
                            draft.street.takeIf { it.isNotBlank() },
                            draft.landmark.takeIf { it.isNotBlank() }?.let { "Near $it" },
                            draft.householdDescription.takeIf { it.isNotBlank() }
                        )
                        binding.tvSummaryWhere.text =
                            if (whereParts.isEmpty()) "Unknown / Not specified" else whereParts.joinToString(", ")

                        binding.tvSummaryWhat.text = draft.incidentType.label
                        binding.tvSummaryWhen.text = DateTimeUtils.formatTimestamp(draft.occurredAtMillis)
                        binding.tvSummaryNeeds.text = if (draft.assistanceNeeds.isEmpty()) {
                            "Not specified"
                        } else {
                            draft.assistanceNeeds.joinToString(", ") { it.label }
                        }
                        binding.tvSummaryExtra.text =
                            draft.extraDetails.ifBlank { "None specified" }
                    }
                }
                launch {
                    viewModel.userProfile.collect { profile ->
                        val draft = viewModel.draft.value
                        renderWhoSummary(draft.whoNeedsHelp, draft.personName, profile.fullName)
                    }
                }
                launch {
                    viewModel.appSettings.collect { settings ->
                        binding.btnSmsMdrrmoReview.text = "Send Text to MDRRMO (${settings.mdrrmoHotline})"
                        binding.btnCallMdrrmoReview.text = "Call MDRRMO (${settings.mdrrmoHotline})"
                    }
                }
                launch {
                    viewModel.emergencyContacts.collect {
                        binding.btnSmsContactReview.text = "Send Text to Someone Else"
                    }
                }
            }
        }
    }

    private fun renderWhoSummary(whoNeedsHelp: WhoNeedsHelp, personName: String, profileFullName: String) {
        val whoText = buildString {
            append(whoNeedsHelp.label)
            if (whoNeedsHelp == WhoNeedsHelp.ME && profileFullName.isNotBlank()) {
                append(" ($profileFullName)")
            } else if (personName.isNotBlank()) {
                append(" ($personName)")
            }
        }
        binding.tvSummaryWho.text = whoText
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = viewModel.draft.value.occurredAtMillis
        }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        viewModel.updateOccurredAt(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
