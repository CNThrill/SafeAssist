package com.safeassist.maramag.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    override fun onPause() {
        super.onPause()
        viewModel.stopSiren()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
