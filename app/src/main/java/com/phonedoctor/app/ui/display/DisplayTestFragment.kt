package com.phonedoctor.app.ui.display

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Display
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
    DARK_GRAY(R.string.display_dark_gray),
    LIGHT_GRAY(R.string.display_light_gray),
    GRAYSCALE(R.string.display_grayscale),
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
                if (currentIndex == steps.lastIndex) {
                    showResultCard()
                } else {
                    goTo(currentIndex + 1)
                }
            }
        }
        binding.buttonResultYes.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.buttonResultNo.setOnClickListener {
            findNavController().navigateUp()
        }

        renderStep()
    }

    private fun goTo(index: Int) {
        if (index !in steps.indices) return
        currentIndex = index
        renderStep()
    }

    private fun renderStep() {
        val step = steps[currentIndex]
        binding.textStep.text = getString(
            R.string.display_step_fmt,
            currentIndex + 1,
            steps.size
        )
        binding.buttonPrevious.isEnabled = currentIndex > 0
        binding.buttonNext.setText(
            if (currentIndex == steps.lastIndex) {
                R.string.common_finish
            } else {
                R.string.common_next
            }
        )

        when (step) {
            DisplayStep.BLACK -> setSolid(Color.BLACK)
            DisplayStep.WHITE -> setSolid(Color.WHITE)
            DisplayStep.RED -> setSolid(Color.RED)
            DisplayStep.GREEN -> setSolid(Color.GREEN)
            DisplayStep.BLUE -> setSolid(Color.BLUE)
            DisplayStep.DARK_GRAY -> setSolid(Color.rgb(32, 32, 32))
            DisplayStep.LIGHT_GRAY -> setSolid(Color.rgb(224, 224, 224))
            DisplayStep.GRAYSCALE -> {
                binding.rootColorSurface.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.BLACK,
                        Color.DKGRAY,
                        Color.GRAY,
                        Color.LTGRAY,
                        Color.WHITE
                    )
                )
            }
            DisplayStep.GRADIENT -> {
                binding.rootColorSurface.background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(
                        Color.RED,
                        Color.YELLOW,
                        Color.GREEN,
                        Color.CYAN,
                        Color.BLUE,
                        Color.MAGENTA
                    )
                )
            }
        }

        val isLightBackground = step == DisplayStep.WHITE ||
            step == DisplayStep.LIGHT_GRAY
        val overlayTextColor = if (isLightBackground) Color.BLACK else Color.WHITE
        binding.buttonClose.setColorFilter(overlayTextColor)
        binding.textStep.setTextColor(overlayTextColor)
    }

    private fun setSolid(color: Int) {
        binding.rootColorSurface.setBackgroundColor(color)
    }

    private fun showResultCard() {
        binding.cardResult.visibility = View.VISIBLE
        binding.bottomControls.visibility = View.GONE
        binding.textDisplayCapabilities.text = buildDisplayCapabilities()
    }

    private fun buildDisplayCapabilities(): String {
        val display = binding.root.display ?: return getString(
            R.string.common_not_available
        )
        val mode = display.mode
        val supportedRefreshRates = display.supportedModes
            .map { it.refreshRate }
            .distinctBy { (it * 100).toInt() }
            .sorted()
            .joinToString(", ") { "%.0f Hz".format(it) }

        val hdrTypes = display.hdrCapabilities.supportedHdrTypes
            .map(::hdrTypeLabel)
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(getString(R.string.display_hdr_none)) }
            .joinToString(", ")

        val wideColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (display.isWideColorGamut) {
                getString(R.string.common_yes)
            } else {
                getString(R.string.common_no)
            }
        } else {
            getString(R.string.common_not_available)
        }

        return getString(
            R.string.display_capabilities_fmt,
            mode.physicalWidth,
            mode.physicalHeight,
            mode.refreshRate,
            supportedRefreshRates,
            hdrTypes,
            wideColor
        )
    }

    private fun hdrTypeLabel(type: Int): String = when (type) {
        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
        else -> "HDR $type"
    }
}
