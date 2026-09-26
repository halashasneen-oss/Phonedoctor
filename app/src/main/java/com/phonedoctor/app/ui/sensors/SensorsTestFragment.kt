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
import com.phonedoctor.app.domain.model.SensorKind
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class SensorsTestFragment : Fragment(R.layout.fragment_sensors_test) {

    private val binding by viewBinding(FragmentSensorsTestBinding::bind)
    private val repository by lazy { SensorsRepository(requireContext().applicationContext) }
    private val adapter = SensorAdapter()

    private val lastTimestampByKind = mutableMapOf<SensorKind, Long>()
    private val smoothedRateByKind = mutableMapOf<SensorKind, Double>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerSensors.adapter = adapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }

        val sensors = repository.getAllSensors()
        adapter.submitList(sensors)

        val available = sensors.count { it.available }
        binding.textSensorsSummary.text = getString(
            R.string.sensors_summary_fmt,
            available,
            sensors.size
        )
        binding.textSensorsNote.setText(R.string.sensors_optional_note)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sensors.filter { it.available }.forEach { entry ->
                    launch {
                        repository.observeReadings(entry.kind).collect { reading ->
                            val rate = updateSampleRate(
                                entry.kind,
                                reading.timestampNanos
                            )
                            adapter.updateReading(
                                entry.kind,
                                SensorLiveUi(
                                    reading = formatReading(reading.values),
                                    accuracy = reading.accuracy,
                                    sampleRateHz = rate
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateSampleRate(
        kind: SensorKind,
        timestampNanos: Long
    ): Double? {
        val previous = lastTimestampByKind.put(kind, timestampNanos) ?: return null
        val delta = timestampNanos - previous
        if (delta <= 0L) return smoothedRateByKind[kind]

        val instantRate = 1_000_000_000.0 / delta.toDouble()
        if (!instantRate.isFinite() || instantRate <= 0.0) {
            return smoothedRateByKind[kind]
        }

        val bounded = instantRate.coerceAtMost(1_000.0)
        val previousSmoothed = smoothedRateByKind[kind]
        val smoothed = if (previousSmoothed == null) {
            bounded
        } else {
            previousSmoothed * 0.8 + bounded * 0.2
        }
        smoothedRateByKind[kind] = smoothed
        return smoothed
    }

    private fun formatReading(values: FloatArray): String = when (values.size) {
        1 -> "%.2f".format(values[0])
        3 -> "x: %.2f  y: %.2f  z: %.2f".format(
            values[0],
            values[1],
            values[2]
        )
        4 -> "x: %.2f  y: %.2f  z: %.2f  w: %.2f".format(
            values[0],
            values[1],
            values[2],
            values[3]
        )
        else -> values.joinToString(", ") { "%.2f".format(it) }
    }
}
