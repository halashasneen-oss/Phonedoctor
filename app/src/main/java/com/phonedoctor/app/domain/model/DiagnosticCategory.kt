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
 * Describes what kind of evidence produced a diagnostic result. A device
 * capability or a temporary state is deliberately different from a measured
 * health observation.
 */
enum class DiagnosticEvidenceType {
    MEASURED,
    CAPABILITY,
    CURRENT_STATE,
    USER_VERIFIED,
    ESTIMATED
}

/**
 * Controls whether a result contributes to the overall health score.
 * INFORMATIONAL results can still be FAIR/POOR to surface a useful warning,
 * but they do not imply hardware damage.
 */
enum class ScoreImpact {
    HEALTH,
    INFORMATIONAL
}

enum class DiagnosticConfidence {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Result of one diagnostic category. Defaults preserve compatibility with
 * reports written by Phone Doctor 1.x.
 */
data class CategoryResult(
    val category: DiagnosticCategory,
    val status: TestStatus,
    val summary: String,
    val detail: String? = null,
    val evidenceType: DiagnosticEvidenceType = DiagnosticEvidenceType.MEASURED,
    val scoreImpact: ScoreImpact = ScoreImpact.HEALTH,
    val confidence: DiagnosticConfidence = DiagnosticConfidence.HIGH
)
