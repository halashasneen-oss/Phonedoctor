package com.phonedoctor.app.data.repository

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraManager
import com.phonedoctor.app.domain.model.CameraCapabilityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraDiagnosticsRepository(private val context: Context) {

    suspend fun getForFacing(front: Boolean): CameraCapabilityInfo? =
        withContext(Dispatchers.Default) {
            getAllInternal().firstOrNull {
                if (front) it.facing == "Front" else it.facing == "Back"
            }
        }

    suspend fun getAll(): List<CameraCapabilityInfo> =
        withContext(Dispatchers.Default) {
            getAllInternal()
        }

    private fun getAllInternal(): List<CameraCapabilityInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        return cameraManager.cameraIdList.mapNotNull { cameraId ->
            runCatching {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val capabilities = characteristics[
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                ].orEmpty()

                val streamMap = characteristics[
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                ]
                val maxJpeg = streamMap
                    ?.getOutputSizes(ImageFormat.JPEG)
                    ?.maxByOrNull { it.width.toLong() * it.height.toLong() }

                val oisModes = characteristics[
                    CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
                ].orEmpty()

                val afModes = characteristics[
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
                ].orEmpty()

                CameraCapabilityInfo(
                    cameraId = cameraId,
                    facing = lensFacingLabel(
                        characteristics[CameraCharacteristics.LENS_FACING]
                    ),
                    hardwareLevel = hardwareLevelLabel(
                        characteristics[
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                        ]
                    ),
                    flashAvailable = characteristics[
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ] == true,
                    focalLengthsMm = characteristics[
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    ]?.toList().orEmpty(),
                    opticalStabilization = oisModes.contains(
                        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                    ),
                    autofocusModes = afModes.map(::afModeLabel).distinct(),
                    rawCapture = capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
                    ),
                    logicalMultiCamera = capabilities.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                    ),
                    maxJpegWidth = maxJpeg?.width,
                    maxJpegHeight = maxJpeg?.height,
                    sensorOrientationDegrees = characteristics[
                        CameraCharacteristics.SENSOR_ORIENTATION
                    ]
                )
            }.getOrNull()
        }
    }

    private fun lensFacingLabel(value: Int?): String = when (value) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    private fun hardwareLevelLabel(value: Int?): String = when (value) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
        else -> "Unknown"
    }

    private fun afModeLabel(mode: Int): String = when (mode) {
        CameraMetadata.CONTROL_AF_MODE_OFF -> "Off"
        CameraMetadata.CONTROL_AF_MODE_AUTO -> "Auto"
        CameraMetadata.CONTROL_AF_MODE_MACRO -> "Macro"
        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "Continuous video"
        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "Continuous picture"
        CameraMetadata.CONTROL_AF_MODE_EDOF -> "EDOF"
        else -> "Mode $mode"
    }
}
