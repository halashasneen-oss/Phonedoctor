package com.phonedoctor.app.domain.model

enum class DiagnosticCategory {
    BATTERY,
    STORAGE,
    MEMORY,
    CPU,
    DISPLAY,
    TOUCH,
    AUDIO,
    MICROPHONE,
    SENSORS,
    CAMERA,
    CONNECTIVITY
}

/**
 * Result of a single category check: a status, a short human-readable summary,
 * and whether the check needed the user to interact (e.g. confirm the display
 * looked correct) versus being fully automatic.
 */
data class CategoryResult(
    val category: DiagnosticCategory,
    val status: TestStatus,
    val summary: String,
    val detail: String? = null
)
