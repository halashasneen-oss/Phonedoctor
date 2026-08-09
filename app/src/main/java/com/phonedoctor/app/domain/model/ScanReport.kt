package com.phonedoctor.app.domain.model

data class ScanReport(
    val id: Long = 0L,
    val timestampMillis: Long,
    val healthScore: Int,
    val results: List<CategoryResult>
) {
    val passedCount: Int get() = results.count { it.status == TestStatus.EXCELLENT || it.status == TestStatus.GOOD }
    val warningCount: Int get() = results.count { it.status == TestStatus.FAIR }
    val failedCount: Int get() = results.count { it.status == TestStatus.POOR }
}
