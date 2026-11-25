package com.safeassist.maramag.ui.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.safeassist.maramag.R
import com.safeassist.maramag.SafeAssistApplication
import com.safeassist.maramag.databinding.FragmentSettingsBinding
import com.safeassist.maramag.viewmodel.SafeAssistViewModelFactory
import com.safeassist.maramag.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        SafeAssistViewModelFactory(requireActivity().application as SafeAssistApplication)
    }

    private var ignoreListeners = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCountdown3.setOnClickListener { viewModel.setSosCountdownSeconds(3) }
        binding.btnCountdown5.setOnClickListener { viewModel.setSosCountdownSeconds(5) }
        binding.btnCountdown10.setOnClickListener { viewModel.setSosCountdownSeconds(10) }

        binding.switchSirenEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!ignoreListeners) {
                viewModel.setSirenEnabled(isChecked)
            }
        }

        binding.btnTestStopSiren.setOnClickListener {
            val started = viewModel.toggleTestSiren()
            if (started) {
                Toast.makeText(
                    requireContext(),
                    "Playing test siren.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.switchGpsPreview.setOnCheckedChangeListener { _, isChecked ->
            if (!ignoreListeners) {
                viewModel.setAskGpsBeforeSending(isChecked)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.appSettings.collect { settings ->
                        ignoreListeners = true
                        styleCountdownButton(binding.btnCountdown3, settings.sosCountdownSeconds == 3)
                        styleCountdownButton(binding.btnCountdown5, settings.sosCountdownSeconds == 5)
                        styleCountdownButton(binding.btnCountdown10, settings.sosCountdownSeconds == 10)

                        binding.switchSirenEnabled.isChecked = settings.sirenEnabled
                        binding.switchGpsPreview.isChecked = settings.askGpsBeforeSending
                        ignoreListeners = false
                    }
                }
                launch {
                    viewModel.isSirenPlaying.collect { playing ->
                        binding.btnTestStopSiren.text = if (playing) {
                            "Stop Siren"
                        } else {
                            "Test Siren"
                        }
                    }
                }
            }
        }
    }

    private fun styleCountdownButton(button: MaterialButton, selected: Boolean) {
        val ctx = requireContext()
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primary_blue))
            button.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.bg_light))
            button.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            button.strokeWidth = 2
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_subtle))
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopSiren()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
