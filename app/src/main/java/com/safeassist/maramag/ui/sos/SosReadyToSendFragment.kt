package com.safeassist.maramag.ui.sos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.communication.CommunicationHelper
import com.safeassist.maramag.databinding.FragmentSosReadyToSendBinding
import com.safeassist.maramag.model.LocationResult
import com.safeassist.maramag.navigation.SafeAssistNavigator
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import com.safeassist.maramag.viewmodel.SosViewModel
import kotlinx.coroutines.launch

class SosReadyToSendFragment : Fragment() {

    private var _binding: FragmentSosReadyToSendBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SosViewModel by activityViewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    private var gpsEnabledInSettings = true
    private var manualGpsPromptRequested = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            manualGpsPromptRequested = false
            viewModel.fetchLocationIfPermitted()
        } else {
            viewModel.markLocationPermissionNeeded()
            val permanentlyDenied = manualGpsPromptRequested &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            manualGpsPromptRequested = false

            if (permanentlyDenied) {
                Toast.makeText(
                    requireContext(),
                    "Allow Location permission in App Settings to include GPS.",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireContext().packageName, null)
                )
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission needed for GPS. Using saved address.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosReadyToSendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goHomeAction = {
            viewModel.stopSiren()
            SafeAssistNavigator.navigateHomeFromSos(findNavController())
        }

        binding.btnSosBackHome.setOnClickListener { goHomeAction() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goHomeAction()
                }
            }
        )

        if (savedInstanceState == null) {
            viewModel.startSosSession()
            gpsEnabledInSettings = viewModel.appSettings.value.askGpsBeforeSending
            if (gpsEnabledInSettings &&
                !viewModel.hasLocationPermission() &&
                viewModel.shouldPromptLocationPermissionThisSos()
            ) {
                viewModel.markLocationPermissionPromptedThisSos()
                manualGpsPromptRequested = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else if (!gpsEnabledInSettings) {
                viewModel.markGpsDisabledInSettings()
            }
        }

        binding.btnStopSirenReady.setOnClickListener {
            viewModel.stopSiren()
        }

        binding.btnRequestGpsPermission.setOnClickListener {
            when {
                !viewModel.hasLocationPermission() -> {
                    manualGpsPromptRequested = true
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                !viewModel.isDeviceLocationEnabled() -> {
                    try {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Turn on Location in device Settings to include GPS.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                else -> {
                    viewModel.fetchLocationIfPermitted()
                }
            }
        }

        binding.chipIncludeGps.setOnClickListener {
            viewModel.toggleIncludeGps()
        }

        binding.btnSendPrefilledSms.setOnClickListener {
            val hotline = viewModel.appSettings.value.mdrrmoHotline
            val message = viewModel.buildSosMessage()
            val res = CommunicationHelper.openSmsApp(requireContext(), hotline, message)
            if (res is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnCallMdrrmoSos.setOnClickListener {
            val hotline = viewModel.appSettings.value.mdrrmoHotline
            val res = CommunicationHelper.openDialer(requireContext(), hotline)
            if (res is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSmsTrustedSos.setOnClickListener {
            val contacts = viewModel.emergencyContacts.value
            val target = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
            val message = viewModel.buildSosMessage()
            val res = CommunicationHelper.openSmsApp(requireContext(), target?.phoneNumber.orEmpty(), message)
            if (res is CommunicationHelper.ActionResult.Error) {
                Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnCopySosText.setOnClickListener {
            val message = viewModel.buildSosMessage()
            when (val res = CommunicationHelper.copyToClipboard(requireContext(), "SafeAssist SOS", message)) {
                is CommunicationHelper.ActionResult.Success ->
                    Toast.makeText(
                        requireContext(),
                        "Message copied. Paste into SMS or Messenger.",
                        Toast.LENGTH_LONG
                    ).show()
                is CommunicationHelper.ActionResult.Error ->
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locationResult.collect { loc ->
                        when (loc) {
                            is LocationResult.Loading -> {
                                binding.btnRequestGpsPermission.visibility = View.GONE
                                if (viewModel.hasLocationPermission() && gpsEnabledInSettings) {
                                    binding.tvGpsCoordinates.text = "Finding my location..."
                                }
                            }
                            is LocationResult.Available -> {
                                binding.btnRequestGpsPermission.visibility = View.GONE
                                binding.tvGpsCoordinates.text = loc.toPlainText()
                            }
                            is LocationResult.Unavailable -> {
                                binding.tvGpsCoordinates.text = loc.reason
                                if (gpsEnabledInSettings) {
                                    binding.btnRequestGpsPermission.visibility = View.VISIBLE
                                    binding.btnRequestGpsPermission.text = when {
                                        !viewModel.hasLocationPermission() -> "Allow Location"
                                        !viewModel.isDeviceLocationEnabled() -> "Turn On Location"
                                        else -> "Retry GPS"
                                    }
                                } else {
                                    binding.btnRequestGpsPermission.visibility = View.GONE
                                }
                            }
                        }
                        refreshMessagePreview()
                    }
                }
                launch {
                    viewModel.includeGpsInMessage.collect { included ->
                        binding.chipIncludeGps.isChecked = included
                        binding.chipIncludeGps.text = if (included) {
                            getString(R.string.gps_included)
                        } else {
                            getString(R.string.gps_excluded)
                        }
                        binding.chipIncludeGps.isEnabled = viewModel.locationResult.value is LocationResult.Available
                        refreshMessagePreview()
                    }
                }
                launch {
                    viewModel.userProfile.collect { profile ->
                        binding.tvSavedLocationFallback.text = "My Saved Address: ${profile.formattedLocation()}"
                        refreshMessagePreview()
                    }
                }
                launch {
                    viewModel.appSettings.collect { settings ->
                        gpsEnabledInSettings = settings.askGpsBeforeSending
                        binding.btnSendPrefilledSms.text = "Send Text to MDRRMO (${settings.mdrrmoHotline})"
                        binding.btnCallMdrrmoSos.text = "Call MDRRMO (${settings.mdrrmoHotline})"
                        refreshMessagePreview()
                    }
                }
                launch {
                    viewModel.emergencyContacts.collect { contacts ->
                        val target = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
                        binding.btnSmsTrustedSos.text = if (target != null) {
                            "Send Text to ${target.name}"
                        } else {
                            "Send Text to Trusted Contact"
                        }
                    }
                }
                launch {
                    viewModel.isSirenPlaying.collect { playing ->
                        binding.cardSirenRinging.visibility = if (playing) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun refreshMessagePreview() {
        binding.tvPrefilledMessagePreview.text = viewModel.buildSosMessage()
    }

    override fun onResume() {
        super.onResume()
        if (gpsEnabledInSettings &&
            viewModel.hasLocationPermission() &&
            viewModel.isDeviceLocationEnabled() &&
            viewModel.locationResult.value is LocationResult.Unavailable
        ) {
            viewModel.fetchLocationIfPermitted()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
