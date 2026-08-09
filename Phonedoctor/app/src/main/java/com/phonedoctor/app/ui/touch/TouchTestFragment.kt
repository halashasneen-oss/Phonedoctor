package com.phonedoctor.app.ui.touch

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentTouchTestBinding
import com.phonedoctor.app.ui.common.viewBinding

class TouchTestFragment : Fragment(R.layout.fragment_touch_test) {

    private val binding by viewBinding(FragmentTouchTestBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.touchGrid.onStatsChanged = { points, coverage ->
            binding.textTouchPoints.text = getString(R.string.touch_points) + ": $points"
            binding.textCoverage.text = getString(R.string.touch_coverage) + ": " + getString(R.string.touch_coverage_fmt, coverage)
        }

        binding.buttonClose.setOnClickListener { findNavController().navigateUp() }
        binding.buttonReset.setOnClickListener { binding.touchGrid.reset() }
        binding.buttonFinish.setOnClickListener { findNavController().navigateUp() }
    }
}
