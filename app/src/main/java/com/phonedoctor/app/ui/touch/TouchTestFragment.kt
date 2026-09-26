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

        binding.touchGrid.onStatsChanged = { stats ->
            binding.textCurrentPointers.text = getString(
                R.string.touch_current_fingers_fmt,
                stats.currentPointers
            )
            binding.textMaxPointers.text = getString(
                R.string.touch_max_fingers_fmt,
                stats.maxSimultaneousPointers
            )
            binding.textCoverage.text = getString(
                R.string.touch_coverage_value_fmt,
                stats.coveragePercent
            )
            binding.textEdgeCoverage.text = getString(
                R.string.touch_edge_coverage_fmt,
                stats.edgeCoveragePercent
            )
            binding.textMissedCells.text = getString(
                R.string.touch_missed_cells_fmt,
                stats.missedCells
            )
        }

        binding.buttonClose.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.buttonReset.setOnClickListener {
            binding.touchGrid.reset()
        }
        binding.buttonFinish.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
