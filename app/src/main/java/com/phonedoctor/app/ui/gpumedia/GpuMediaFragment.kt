package com.phonedoctor.app.ui.gpumedia

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.FragmentGpuMediaBinding
import com.phonedoctor.app.domain.model.CodecSupportSummary
import com.phonedoctor.app.domain.model.GpuInfo
import com.phonedoctor.app.domain.model.MediaDiagnosticsInfo
import com.phonedoctor.app.ui.common.InfoRow
import com.phonedoctor.app.ui.common.InfoRowAdapter
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class GpuMediaFragment : Fragment(R.layout.fragment_gpu_media) {

    private val binding by viewBinding(FragmentGpuMediaBinding::bind)
    private val gpuAdapter = InfoRowAdapter()
    private val mediaAdapter = InfoRowAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerGpu.adapter = gpuAdapter
        binding.recyclerMedia.adapter = mediaAdapter
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.buttonRefresh.setOnClickListener { refresh() }

        refresh()
    }

    private fun refresh() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.buttonRefresh.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val repository = serviceLocator().gpuMediaRepository
            val gpuDeferred = async { repository.getGpuInfo() }
            val mediaDeferred = async { repository.getMediaInfo() }

            val gpu = gpuDeferred.await()
            val media = mediaDeferred.await()
            renderGpu(gpu)
            renderMedia(media)

            binding.progressLoading.visibility = View.GONE
            binding.buttonRefresh.isEnabled = true
        }
    }

    private fun renderGpu(info: GpuInfo) {
        val na = getString(R.string.common_not_available)
        gpuAdapter.submitList(
            listOf(
                InfoRow(
                    getString(R.string.gpu_vendor),
                    info.vendor ?: na
                ),
                InfoRow(
                    getString(R.string.gpu_renderer),
                    info.renderer ?: na
                ),
                InfoRow(
                    getString(R.string.gpu_opengl_version),
                    info.openGlVersion ?: na
                ),
                InfoRow(
                    getString(R.string.gpu_glsl_version),
                    info.shadingLanguageVersion ?: na
                ),
                InfoRow(
                    getString(R.string.gpu_vulkan_hardware_level),
                    info.vulkanHardwareLevelVersion?.toString() ?: na
                ),
                InfoRow(
                    getString(R.string.gpu_vulkan_version),
                    info.vulkanHardwareVersion?.let(::formatVulkanVersion) ?: na
                )
            )
        )
    }

    private fun renderMedia(info: MediaDiagnosticsInfo) {
        val na = getString(R.string.common_not_available)
        val rows = mutableListOf(
            InfoRow(
                getString(R.string.media_total_codecs),
                info.totalCodecs.toString()
            ),
            InfoRow(
                getString(R.string.media_decoders_encoders),
                getString(
                    R.string.media_decoders_encoders_fmt,
                    info.decoderCount,
                    info.encoderCount
                )
            ),
            InfoRow(
                getString(R.string.media_hardware_codecs),
                info.hardwareAcceleratedCount?.toString() ?: na
            ),
            InfoRow(
                getString(R.string.media_software_codecs),
                info.softwareOnlyCount?.toString() ?: na
            ),
            InfoRow(
                getString(R.string.media_vendor_codecs),
                info.vendorCodecCount?.toString() ?: na
            )
        )

        info.keyCodecSupport.forEach { summary ->
            rows += InfoRow(
                codecLabel(summary.mimeType),
                codecSummary(summary)
            )
        }

        mediaAdapter.submitList(rows)
    }

    private fun codecSummary(summary: CodecSupportSummary): String {
        val hardware = if (
            summary.hardwareDecoderCount != null &&
            summary.hardwareEncoderCount != null
        ) {
            getString(
                R.string.media_codec_hardware_fmt,
                summary.hardwareDecoderCount,
                summary.hardwareEncoderCount
            )
        } else {
            getString(R.string.common_not_available)
        }

        return getString(
            R.string.media_codec_summary_fmt,
            summary.decoderCount,
            summary.encoderCount,
            hardware
        )
    }

    private fun codecLabel(mimeType: String): String = when (mimeType) {
        "video/avc" -> "H.264 / AVC"
        "video/hevc" -> "H.265 / HEVC"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        "audio/mp4a-latm" -> "AAC"
        else -> mimeType
    }

    private fun formatVulkanVersion(version: Int): String {
        val major = version ushr 22
        val minor = (version ushr 12) and 0x3ff
        val patch = version and 0xfff
        return "$major.$minor.$patch"
    }
}
