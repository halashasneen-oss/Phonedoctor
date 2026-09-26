package com.phonedoctor.app.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import com.phonedoctor.app.domain.model.CodecSupportSummary
import com.phonedoctor.app.domain.model.GpuInfo
import com.phonedoctor.app.domain.model.MediaDiagnosticsInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GpuMediaRepository(private val context: Context) {

    suspend fun getGpuInfo(): GpuInfo = withContext(Dispatchers.Default) {
        val gl = queryOpenGl()
        val features = context.packageManager.systemAvailableFeatures

        val vulkanLevel = features.firstOrNull {
            it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
        }?.version
        val vulkanVersion = features.firstOrNull {
            it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION
        }?.version

        GpuInfo(
            vendor = gl.vendor,
            renderer = gl.renderer,
            openGlVersion = gl.version,
            shadingLanguageVersion = gl.shadingLanguageVersion,
            vulkanHardwareLevelVersion = vulkanLevel,
            vulkanHardwareVersion = vulkanVersion
        )
    }

    suspend fun getMediaInfo(): MediaDiagnosticsInfo =
        withContext(Dispatchers.Default) {
            val codecs = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.toList()

            val hardwareCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                codecs.count { it.isHardwareAccelerated }
            } else {
                null
            }
            val softwareCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                codecs.count { it.isSoftwareOnly }
            } else {
                null
            }
            val vendorCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                codecs.count { it.isVendor }
            } else {
                null
            }

            val keyMimes = listOf(
                "video/avc",
                "video/hevc",
                "video/x-vnd.on2.vp9",
                "video/av01",
                "audio/mp4a-latm"
            )

            val summaries = keyMimes.map { mime ->
                val matching = codecs.filter {
                    it.supportedTypes.any { type ->
                        type.equals(mime, ignoreCase = true)
                    }
                }
                val decoders = matching.filterNot { it.isEncoder }
                val encoders = matching.filter { it.isEncoder }

                CodecSupportSummary(
                    mimeType = mime,
                    decoderCount = decoders.size,
                    encoderCount = encoders.size,
                    hardwareDecoderCount = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ) {
                        decoders.count { it.isHardwareAccelerated }
                    } else {
                        null
                    },
                    hardwareEncoderCount = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ) {
                        encoders.count { it.isHardwareAccelerated }
                    } else {
                        null
                    }
                )
            }

            MediaDiagnosticsInfo(
                totalCodecs = codecs.size,
                decoderCount = codecs.count { !it.isEncoder },
                encoderCount = codecs.count { it.isEncoder },
                hardwareAcceleratedCount = hardwareCount,
                softwareOnlyCount = softwareCount,
                vendorCodecCount = vendorCount,
                keyCodecSupport = summaries
            )
        }

    private fun queryOpenGl(): GlStrings {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return GlStrings()

        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            return GlStrings()
        }

        val configAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE,
            EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE,
            EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE,
            8,
            EGL14.EGL_GREEN_SIZE,
            8,
            EGL14.EGL_BLUE_SIZE,
            8,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)

        if (
            !EGL14.eglChooseConfig(
                display,
                configAttributes,
                0,
                configs,
                0,
                configs.size,
                configCount,
                0
            ) ||
            configCount[0] == 0 ||
            configs[0] == null
        ) {
            EGL14.eglTerminate(display)
            return GlStrings()
        }

        val config = configs[0] ?: run {
            EGL14.eglTerminate(display)
            return GlStrings()
        }

        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION,
            2,
            EGL14.EGL_NONE
        )
        val eglContext = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            contextAttributes,
            0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglTerminate(display)
            return GlStrings()
        }

        val pbufferAttributes = intArrayOf(
            EGL14.EGL_WIDTH,
            1,
            EGL14.EGL_HEIGHT,
            1,
            EGL14.EGL_NONE
        )
        val surface = EGL14.eglCreatePbufferSurface(
            display,
            config,
            pbufferAttributes,
            0
        )
        if (surface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
            return GlStrings()
        }

        return try {
            if (
                !EGL14.eglMakeCurrent(
                    display,
                    surface,
                    surface,
                    eglContext
                )
            ) {
                GlStrings()
            } else {
                GlStrings(
                    vendor = GLES20.glGetString(GLES20.GL_VENDOR),
                    renderer = GLES20.glGetString(GLES20.GL_RENDERER),
                    version = GLES20.glGetString(GLES20.GL_VERSION),
                    shadingLanguageVersion = GLES20.glGetString(
                        GLES20.GL_SHADING_LANGUAGE_VERSION
                    )
                )
            }
        } finally {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
        }
    }

    private data class GlStrings(
        val vendor: String? = null,
        val renderer: String? = null,
        val version: String? = null,
        val shadingLanguageVersion: String? = null
    )
}
