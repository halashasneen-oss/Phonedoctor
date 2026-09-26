package com.phonedoctor.app.domain.model

data class GpuInfo(
    val vendor: String?,
    val renderer: String?,
    val openGlVersion: String?,
    val shadingLanguageVersion: String?,
    val vulkanHardwareLevelVersion: Int?,
    val vulkanHardwareVersion: Int?
)

data class CodecSupportSummary(
    val mimeType: String,
    val decoderCount: Int,
    val encoderCount: Int,
    val hardwareDecoderCount: Int?,
    val hardwareEncoderCount: Int?
)

data class MediaDiagnosticsInfo(
    val totalCodecs: Int,
    val decoderCount: Int,
    val encoderCount: Int,
    val hardwareAcceleratedCount: Int?,
    val softwareOnlyCount: Int?,
    val vendorCodecCount: Int?,
    val keyCodecSupport: List<CodecSupportSummary>
)
