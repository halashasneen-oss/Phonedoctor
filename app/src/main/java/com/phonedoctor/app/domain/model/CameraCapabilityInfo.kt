package com.phonedoctor.app.domain.model

data class CameraCapabilityInfo(
    val cameraId: String,
    val facing: String,
    val hardwareLevel: String,
    val flashAvailable: Boolean,
    val focalLengthsMm: List<Float>,
    val opticalStabilization: Boolean,
    val autofocusModes: List<String>,
    val rawCapture: Boolean,
    val logicalMultiCamera: Boolean,
    val maxJpegWidth: Int?,
    val maxJpegHeight: Int?,
    val sensorOrientationDegrees: Int?
)
