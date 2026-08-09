package com.phonedoctor.app.ui.sensors

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.data.repository.SensorsRepository
import com.phonedoctor.app.databinding.FragmentSensorsTestBinding
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class SensorsTestFragment : Fragment(R.layout.fragment_sensors_test) {

    private val binding by viewBinding(FragmentSensorsTestBinding::bind)
    private val repository by lazy { SensorsRepository(requireContext().applicationContext) }
    private val adapter = SensorAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerSensors.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        val sensors = repository.getAllSensors()
        adapter.submitList(sensors)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sensors.filter { it.available }.forEach { entry ->
                    launch {
                        repository.observeReadings(entry.kind).collect { values ->
                            adapter.updateReading(entry.kind, formatReading(values))
                        }
                    }
                }
            }
        }
    }

    private fun formatReading(values: FloatArray): String = when (values.size) {
        1 -> "%.2f".format(values[0])
        3 -> "x: %.2f  y: %.2f  z: %.2f".format(values[0], values[1], values[2])
        4 -> "x: %.2f  y: %.2f  z: %.2f  w: %.2f".format(values[0], values[1], values[2], values[3])
        else -> values.joinToString(", ") { "%.2f".format(it) }
    }
}
