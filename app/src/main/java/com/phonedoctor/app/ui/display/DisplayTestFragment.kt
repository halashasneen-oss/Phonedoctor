package com.phonedoctor.app.ui.display

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentDisplayTestBinding
import com.phonedoctor.app.ui.common.viewBinding

private enum class DisplayStep(val labelRes: Int) {
    BLACK(R.string.display_black),
    WHITE(R.string.display_white),
    RED(R.string.display_red),
    GREEN(R.string.display_green),
    BLUE(R.string.display_blue),
    GRADIENT(R.string.display_gradient)
}

class DisplayTestFragment : Fragment(R.layout.fragment_display_test) {

    private val binding by viewBinding(FragmentDisplayTestBinding::bind)
    private val steps = DisplayStep.entries
    private var currentIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonClose.setOnClickListener { findNavController().navigateUp() }
        binding.buttonPrevious.setOnClickListener { goTo(currentIndex - 1) }
        binding.buttonNext.setOnClickListener {
            if (currentIndex == steps.lastIndex) {
                showResultCard()
            } else {
                goTo(currentIndex + 1)
            }
        }
        binding.rootColorSurface.setOnClickListener {
            if (binding.cardResult.visibility != View.VISIBLE) {
                if (currentIndex == steps.lastIndex) showResultCard() else goTo(currentIndex + 1)
            }
        }
        binding.buttonResultYes.setOnClickListener { findNavController().navigateUp() }
        binding.buttonResultNo.setOnClickListener { findNavController().navigateUp() }

        renderStep()
    }

    private fun goTo(index: Int) {
        if (index !in steps.indices) return
        currentIndex = index
        renderStep()
    }

    private fun renderStep() {
        val step = steps[currentIndex]
        binding.textStep.text = getString(R.string.display_step_fmt, currentIndex + 1, steps.size)
        binding.buttonPrevious.isEnabled = currentIndex > 0
        binding.buttonNext.setText(
            if (currentIndex == steps.lastIndex) R.string.common_finish else R.string.common_next
        )

        when (step) {
            DisplayStep.BLACK -> binding.rootColorSurface.setBackgroundColor(Color.BLACK)
            DisplayStep.WHITE -> binding.rootColorSurface.setBackgroundColor(Color.WHITE)
            DisplayStep.RED -> binding.rootColorSurface.setBackgroundColor(Color.RED)
            DisplayStep.GREEN -> binding.rootColorSurface.setBackgroundColor(Color.GREEN)
            DisplayStep.BLUE -> binding.rootColorSurface.setBackgroundColor(Color.BLUE)
            DisplayStep.GRADIENT -> {
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
                )
                binding.rootColorSurface.background = gradient
            }
        }

        val isDarkBackground = step != DisplayStep.WHITE
        val overlayTextColor = if (isDarkBackground) Color.WHITE else Color.BLACK
        binding.buttonClose.setColorFilter(overlayTextColor)
        binding.textStep.setTextColor(overlayTextColor)
    }

    private fun showResultCard() {
        binding.cardResult.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.GONE
    }
}
