package com.safeassist.maramag.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.communication.CommunicationHelper
import com.safeassist.maramag.databinding.FragmentEmergencyContactsBinding
import com.safeassist.maramag.databinding.ItemTrustedContactBinding
import com.safeassist.maramag.model.EmergencyContact
import com.safeassist.maramag.utils.EmergencyMessageFormatter
import com.safeassist.maramag.viewmodel.ContactsViewModel
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import kotlinx.coroutines.launch

class EmergencyContactsFragment : Fragment() {

    private var _binding: FragmentEmergencyContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactsViewModel by viewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCallMdrrmo.setOnClickListener {
            val number = viewModel.appSettings.value.mdrrmoHotline
            val result = CommunicationHelper.openDialer(requireContext(), number)
            if (result is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnCopyMdrrmoNumber.setOnClickListener {
            val number = viewModel.appSettings.value.mdrrmoHotline
            when (val res = CommunicationHelper.copyToClipboard(requireContext(), "MDRRMO Hotline", number)) {
                is CommunicationHelper.ActionResult.Success ->
                    Toast.makeText(requireContext(), "Copied $number", Toast.LENGTH_SHORT).show()
                is CommunicationHelper.ActionResult.Error ->
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnEditHotline.setOnClickListener {
            showEditHotlineDialog()
        }

        binding.btnCopyMessengerSummary.setOnClickListener {
            val summary = EmergencyMessageFormatter.formatSosMessage(
                profile = viewModel.userProfile.value,
                caregiverMode = viewModel.appSettings.value.caregiverModeEnabled,
                locationResult = null,
                includeGps = false
            )
            when (val res = CommunicationHelper.copyToClipboard(requireContext(), "SafeAssist Message", summary)) {
                is CommunicationHelper.ActionResult.Success ->
                    Toast.makeText(
                        requireContext(),
                        "Message copied. Paste it into Messenger.",
                        Toast.LENGTH_LONG
                    ).show()
                is CommunicationHelper.ActionResult.Error ->
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnAddTrustedContact.setOnClickListener {
            showAddTrustedContactDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.appSettings.collect { settings ->
                        binding.tvMdrrmoNumber.text = settings.mdrrmoHotline
                    }
                }
                launch {
                    viewModel.emergencyContacts.collect { contacts ->
                        renderTrustedContacts(contacts)
                    }
                }
            }
        }
    }

    private fun renderTrustedContacts(contacts: List<EmergencyContact>) {
        binding.containerTrustedContacts.removeAllViews()
        binding.tvNoTrustedContacts.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(requireContext())
        for (contact in contacts) {
            val itemBinding = ItemTrustedContactBinding.inflate(
                inflater,
                binding.containerTrustedContacts,
                false
            )
            itemBinding.tvContactName.text = contact.name
            itemBinding.tvContactRole.text = contact.relationshipOrRole.ifBlank { "Trusted Contact" }
            itemBinding.tvContactPhone.text = contact.phoneNumber

            itemBinding.btnCallTrusted.setOnClickListener {
                val res = CommunicationHelper.openDialer(requireContext(), contact.phoneNumber)
                if (res is CommunicationHelper.ActionResult.Error) {
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                }
            }

            itemBinding.btnDeleteTrusted.setOnClickListener {
                viewModel.deleteContact(contact)
                Toast.makeText(requireContext(), "Removed ${contact.name}", Toast.LENGTH_SHORT).show()
            }

            binding.containerTrustedContacts.addView(itemBinding.root)
        }
    }

    private fun showEditHotlineDialog() {
        val input = EditText(requireContext()).apply {
            setText(viewModel.appSettings.value.mdrrmoHotline)
            hint = "Phone number"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit MDRRMO Number")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                viewModel.updateMdrrmoHotline(input.text.toString())
                Toast.makeText(requireContext(), "Number updated.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTrustedContactDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val etName = EditText(requireContext()).apply { hint = "Name" }
        val etRole = EditText(requireContext()).apply { hint = "Relationship (e.g., Mother, Neighbor)" }
        val etPhone = EditText(requireContext()).apply {
            hint = "Phone number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        layout.addView(etName)
        layout.addView(etRole)
        layout.addView(etPhone)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Trusted Contact")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val role = etRole.text.toString().trim()
                if (name.isBlank() || phone.isBlank()) {
                    Toast.makeText(requireContext(), "Name and phone number are required.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.saveContact(
                    EmergencyContact(
                        name = name,
                        relationshipOrRole = role.ifBlank { "Trusted Contact" },
                        phoneNumber = phone
                    )
                )
                Toast.makeText(requireContext(), "Saved $name.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
